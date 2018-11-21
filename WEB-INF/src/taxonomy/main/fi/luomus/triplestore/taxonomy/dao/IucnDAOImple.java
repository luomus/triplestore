package fi.luomus.triplestore.taxonomy.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.methods.HttpGet;

import fi.luomus.commons.config.Config;
import fi.luomus.commons.containers.Area;
import fi.luomus.commons.containers.IucnRedListInformalTaxonGroup;
import fi.luomus.commons.containers.LocalizedText;
import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.ObjectLiteral;
import fi.luomus.commons.containers.rdf.ObjectResource;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.RdfProperties;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.db.connectivity.TransactionConnection;
import fi.luomus.commons.http.HttpClientService;
import fi.luomus.commons.json.JSONObject;
import fi.luomus.commons.reporting.ErrorReporter;
import fi.luomus.commons.taxonomy.Occurrences.Occurrence;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.taxonomy.iucn.EndangermentObject;
import fi.luomus.commons.taxonomy.iucn.Evaluation;
import fi.luomus.commons.taxonomy.iucn.HabitatObject;
import fi.luomus.commons.utils.SingleObjectCache;
import fi.luomus.commons.utils.SingleObjectCache.CacheLoader;
import fi.luomus.commons.utils.URIBuilder;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.dao.SearchParams;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.dao.TriplestoreDAOConst;
import fi.luomus.triplestore.models.UsedAndGivenStatements;
import fi.luomus.triplestore.taxonomy.iucn.model.Container;
import fi.luomus.triplestore.taxonomy.iucn.model.EditHistory;
import fi.luomus.triplestore.taxonomy.iucn.model.EditHistory.EditHistoryEntry;
import fi.luomus.triplestore.taxonomy.iucn.model.Editors;
import fi.luomus.triplestore.taxonomy.iucn.model.EvaluationTarget;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

public class IucnDAOImple implements IucnDAO {

	private static final Predicate TYPE_OF_OCCURRENCE_IN_FINLAND_PREDICATE = new Predicate("MX.typeOfOccurrenceInFinland");
	private static final String TRUE = "true";
	private static final int PAGE_SIZE_TAXON_LIST = 3000;
	private static final String INFORMAL_GROUP_FILTERS = "informalGroupFilters";
	private static final String INCLUDE_HIDDEN = "includeHidden";
	private static final Set<String> DEV_LIMITED_TO_INFORMAL_GROUPS = Utils.set("MVL.301", "MVL.1");
	private static final String SORT_ORDER = "sortOrder";
	private static final String MO_STATUS = "MO.status";
	private static final String MO_AREA = "MO.area";
	private static final String MO_YEAR = "MO.year";
	private static final String MO_THREATENED = "MO.threatened";

	private static final String SCHEMA = TriplestoreDAOConst.SCHEMA;

	private static final String EDIT_HISTORY_SQL = "" + 
			" SELECT 	notesliteral.RESOURCELITERAL, userqname.RESOURCENAME " + 
			" FROM 		"+SCHEMA+".rdf_statement_history notes " + 
			" JOIN		"+SCHEMA+".rdf_resource notesliteral ON (notes.objectfk = notesliteral.resourceid) " + 
			" JOIN		"+SCHEMA+".rdf_resource userqname ON (notes.userfk = userqname.resourceid) " + 
			" WHERE		notes.SUBJECTFK = (select resourceid from "+SCHEMA+".rdf_resource where resourcename = ?) " + 
			" AND		notes.predicatefk = (select resourceid from "+SCHEMA+".rdf_resource where resourcename = '"+Evaluation.EDIT_NOTES+"') " + 
			" ORDER BY	notes.created DESC ";

	private static final String ML_NAME = "ML.name";
	private static final Qname EVALUATION_AREA_TYPE_QNAME = new Qname("ML.iucnEvaluationArea");
	private static final String AREA_TYPE = "ML.areaType";
	private static final String AREA = "ML.area";
	private static final String BIOTA_QNAME = "MX.37600";
	private static final String QNAME = "qname";
	private static final String ONLY_FINNISH = "onlyFinnish";
	private static final String SELECTED_FIELDS = "selectedFields";
	private static final String PAGE_SIZE = "pageSize";
	private static final String PAGE = "page";
	private static final String NEXT_PAGE = "nextPage";
	private static final String RESULTS = "results";

	private static final Object EVAL_LOAD_LOCK = new Object();

	private final Config config;
	private final TriplestoreDAO triplestoreDAO;
	private final ExtendedTaxonomyDAO taxonomyDAO;
	private Container container;
	private final ErrorReporter errorReporter;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private final boolean devMode;
	private boolean initialEvaluationsLoaded = false;


	public IucnDAOImple(Config config, boolean devMode, TriplestoreDAO triplestoreDAO, ExtendedTaxonomyDAO taxonomyDAO, ErrorReporter errorReporter) {
		System.out.println("Creating " +  IucnDAOImple.class.getName());
		this.config = config;
		this.devMode = devMode;
		this.triplestoreDAO = triplestoreDAO;
		this.taxonomyDAO = taxonomyDAO;
		this.container = new Container(this);
		this.errorReporter = errorReporter;
		startNightlyScheduler();
	}

	public void close() {
		try {
			if (scheduler != null) {
				scheduler.shutdownNow();
			}
		} catch (Exception e) {}
	}

	private final Runnable iucnContainerReinitializer = new Runnable() {
		@Override
		public void run() {
			synchronized (EVAL_LOAD_LOCK) {
				System.out.println("IUCN container being reinitialized...");
				try {
					initialEvaluationsLoaded = false;
					container = new Container(IucnDAOImple.this);
					for (String groupQname : getGroupEditors().keySet()) {
						container.getTargetsOfGroup(groupQname);
					}
				} catch (Exception e) {
					errorReporter.report("IUCN container reinitializing", e);
				}
				System.out.println("Reinitializing IUCN container completed!");
			}
		}
	};

	private final Runnable iucnRedListTaxonGroupNameUpdater = new Runnable() {
		@Override
		public void run() {
			System.out.println("Staring to update IUCN Red List Taxon Group names...");
			try {
				for (IucnRedListInformalTaxonGroup group : taxonomyDAO.getIucnRedListInformalTaxonGroupsForceReload().values()) {
					Model dbGroup = triplestoreDAO.get(group.getQname());
					if (namesDoNotMatch(group, dbGroup)) {
						triplestoreDAO.storeIucnRedListTaxonGroup(group);
						System.out.println("Updated name of group " + group.getQname() + " to " + group.getName());
					}
				}
			} catch (Exception e) {
				errorReporter.report("Updating IUCN Red List Taxon Group names", e);
			}
			System.out.println("IUCN Red List Taxon Group names updated!");
		}

		private boolean namesDoNotMatch(IucnRedListInformalTaxonGroup group, Model dbGroup) {
			for (Map.Entry<String, String> e : group.getName().getAllTexts().entrySet()) {
				String locale = e.getKey();
				if (locale == null) locale = "";
				String name = e.getValue();
				String dbName = getName(locale, dbGroup);
				if (!dbName.equals(name)) {
					return true;
				}
			}
			return false;
		}

		private String getName(String locale, Model dbGroup) {
			for (Statement s : dbGroup.getStatements("MVL.name")) {
				if (s.isResourceStatement()) continue;
				if (locale.equals(s.getObjectLiteral().getLangcode())) {
					String name = s.getObjectLiteral().getContent(); 
					if (name == null) return "";
					return name;
				}
			}
			return "";
		}
	};

	private final Runnable taxonDataSynchronizer = new Runnable() {
		@Override
		public void run() {
			System.out.println("Starting to synchronize taxon data with IUCN data...");
			try {
				container.makeSureEvaluationDataIsLoaded();
				int c = 1;
				for (EvaluationTarget target : container.getTargets()) {
					if (target.getQname() == null || target.getQname().isEmpty()) {
						errorReporter.report("Syncing taxon data with IUCN data: Null target taxon qname: " + debug(target));
						continue;
					}
					Qname speciesQname = new Qname(target.getQname());
					if (!taxonomyDAO.getTaxonContainer().hasTaxon(speciesQname)) {
						errorReporter.report("Syncing taxon data with IUCN data: Taxon not found: " + speciesQname + " for target " + target);
						continue;
					}
					EditableTaxon taxon = (EditableTaxon) taxonomyDAO.getTaxon(speciesQname);
					if (c++ % 5000 == 0) System.out.println(" ... syncing " + c);
					boolean statusesChanged = syncRedListStatuses(target, taxon);
					boolean habitatsChanged = syncHabitats(target, taxon);
					boolean typeOfOccurrenceChanged = syncTypeOfOccurrence(target, taxon);
					if (statusesChanged || habitatsChanged || typeOfOccurrenceChanged) {
						taxon.invalidateSelf();
					}
				}
			} catch (Exception e) {
				errorReporter.report("Syncing taxon data with IUCN data", e);
			}
			System.out.println("Synchronizing taxon data with IUCN data completed!");
		}

		private boolean syncTypeOfOccurrence(EvaluationTarget target, EditableTaxon taxon) throws Exception {
			if (!taxon.getTypesOfOccurrenceInFinland().isEmpty()) return false;

			Evaluation evaluation = getLatestReadyEvaluation(target);
			if (evaluation == null) return false;

			Qname typeOfOccurrenceInFinland = new Qname(evaluation.getValue(Evaluation.TYPE_OF_OCCURRENCE_IN_FINLAND));
			if (!given(typeOfOccurrenceInFinland)) return false;

			updateTypesOfOccurrenceInFinland(taxon, typeOfOccurrenceInFinland);
			return true;
		}

		private boolean syncHabitats(EvaluationTarget target, EditableTaxon taxon) throws Exception {
			Evaluation evaluation = getLatestReadyEvaluation(target);
			if (evaluation == null) return false;
			if (evaluation.getPrimaryHabitat() == null) return false;
			
			taxonomyDAO.addHabitats(taxon);
			if (taxon.getPrimaryHabitat() != null && !isNewestPossible(evaluation)) {
				return false; // don't override existing taxon data with old evaluation data
			}

			updateHabitats(taxon, evaluation.getPrimaryHabitat(), evaluation.getSecondaryHabitats());
			return true;
		}

		private boolean isNewestPossible(Evaluation evaluation) throws Exception {
			return evaluation.getEvaluationYear().equals(newestEvaluationYear());
		}

		private Integer newestEvaluationYear() throws Exception {
			return getEvaluationYears().iterator().next();
		}

		private void updateHabitats(EditableTaxon taxon, HabitatObject primaryHabitat, List<HabitatObject> secondaryHabitats) throws Exception {
			UsedAndGivenStatements statements = new UsedAndGivenStatements();
			statements.addUsed(IucnDAO.PRIMARY_HABITAT_PREDICATE, null, null);
			statements.addUsed(IucnDAO.SECONDARY_HABITAT_PREDICATE, null, null);
			if (primaryHabitat != null) {
				statements.addStatement(new Statement(IucnDAO.PRIMARY_HABITAT_PREDICATE, new ObjectResource(primaryHabitat.getId())));
			}
			System.out.println("   " + taxon.getQname() + " " + IucnDAO.PRIMARY_HABITAT_PREDICATE + " -> " + primaryHabitat);
			for (HabitatObject h : secondaryHabitats) {
				statements.addStatement(new Statement(IucnDAO.SECONDARY_HABITAT_PREDICATE, new ObjectResource(h.getId())));
				System.out.println("   " + taxon.getQname() + " " + IucnDAO.SECONDARY_HABITAT_PREDICATE + " -> " + h);
			}
			triplestoreDAO.store(new Subject(taxon.getQname()), statements);
		}

		private Evaluation getLatestReadyEvaluation(EvaluationTarget target) {
			for (Evaluation evaluation : target.getEvaluations()) {
				if (!evaluation.isReady()) continue;
				return evaluation;
			}
			return null;
		}

		private boolean syncRedListStatuses(EvaluationTarget target, EditableTaxon taxon) throws Exception {
			boolean modifiedTaxon = false;
			for (Evaluation evaluation : target.getEvaluations()) {
				if (!evaluation.isLocked()) continue;
				if (!evaluation.hasIucnStatus()) continue;

				Integer year = evaluation.getEvaluationYear();
				Qname status = new Qname(evaluation.getIucnStatus());
				Qname taxonRedListStatus = taxon.getRedListStatusForYear(year);

				if (!status.equals(taxonRedListStatus)) {
					updateRedListStatus(taxon, year, status);
					modifiedTaxon = true;
				}
			}
			return modifiedTaxon;
		}

		private String debug(EvaluationTarget target) {
			StringBuilder b = new StringBuilder();
			b.append("[");
			for (Evaluation e : target.getEvaluations()) {
				b.append("[").append(e.getId()).append(" ").append(e.getEvaluationYear()).append("]");
			}
			b.append("]");
			return b.toString();
		}

		private void updateTypesOfOccurrenceInFinland(EditableTaxon taxon, Qname typeOfOccurrenceInFinland) throws Exception {
			UsedAndGivenStatements statements = new UsedAndGivenStatements();
			statements.addUsed(TYPE_OF_OCCURRENCE_IN_FINLAND_PREDICATE, null, null);
			statements.addStatement(new Statement(TYPE_OF_OCCURRENCE_IN_FINLAND_PREDICATE, new ObjectResource(typeOfOccurrenceInFinland)));
			triplestoreDAO.store(new Subject(taxon.getQname()), statements);
			System.out.println("   " + taxon.getQname() + " " + TYPE_OF_OCCURRENCE_IN_FINLAND_PREDICATE + " -> " + typeOfOccurrenceInFinland);
		}

		private void updateRedListStatus(EditableTaxon taxon, Integer year, Qname status) throws Exception {
			Predicate statusPredicate = new Predicate("MX.redListStatus"+year+"Finland");
			triplestoreDAO.store(new Subject(taxon.getQname()), new Statement(statusPredicate, new ObjectResource(status)));
			System.out.println("   " + taxon.getQname() + " " + statusPredicate + " -> " + status);
		}

		private boolean given(Qname qname) {
			return qname != null && qname.isSet();
		}
	};

	private void startNightlyScheduler() {
		int repeatPeriod24H = 24 * 60;
		long intitialDelay3am = calculateInitialDelayTill(3, repeatPeriod24H);
		long intitialDelay6am = calculateInitialDelayTill(6, repeatPeriod24H);

		scheduler.scheduleAtFixedRate(
				taxonDataSynchronizer, 
				intitialDelay3am, repeatPeriod24H, TimeUnit.MINUTES);

		scheduler.scheduleAtFixedRate(iucnRedListTaxonGroupNameUpdater, 
				intitialDelay3am, repeatPeriod24H, TimeUnit.MINUTES);

		scheduler.scheduleAtFixedRate(
				iucnContainerReinitializer, 
				intitialDelay6am, repeatPeriod24H, TimeUnit.MINUTES);
	}

	private long calculateInitialDelayTill(int hour, int repeatPeriod24H) {
		Calendar now = Calendar.getInstance();
		now.setTime(new Date());
		int hoursNow = now.get(Calendar.HOUR_OF_DAY);
		int minutesNow = now.get(Calendar.MINUTE);

		int minutesPassed12AM = hoursNow * 60 + minutesNow;
		int minutesAtHour = hour * 60;

		long initialDelay = minutesPassed12AM <= minutesAtHour ? minutesAtHour - minutesPassed12AM : repeatPeriod24H - (minutesPassed12AM - minutesAtHour);
		return initialDelay;
	}

	private final SingleObjectCache<Map<String, Editors>> 
	cachedGroupEditors = 
	new SingleObjectCache<>(
			new CacheLoader<Map<String, Editors>>() {
				@Override
				public Map<String, Editors> load() {
					try {
						Map<String, Editors> map = new HashMap<>();
						for (Model m : triplestoreDAO.getSearchDAO().search(new SearchParams().type("MKV.taxonGroupIucnEditors"))) {
							Editors editors = new Editors(new Qname(m.getSubject().getQname()));
							if (!m.hasStatements("MKV.taxonGroup")) continue;
							String groupQname = m.getStatements("MKV.taxonGroup").get(0).getObjectResource().getQname();
							for (Statement editor : m.getStatements("MKV.iucnEditor")) {
								editors.addEditor(new Qname(editor.getObjectResource().getQname()));
							}
							if (!editors.getEditors().isEmpty()) {
								map.put(groupQname, editors);
							}
						}
						return map;
					} catch (Exception e) {
						throw new RuntimeException("Cached group editors", e);
					}
				}
			}, 5*60);

	@Override
	public Map<String, Editors> getGroupEditors() throws Exception {
		return cachedGroupEditors.get();
	}

	@Override
	public void clearEditorCache() {
		cachedGroupEditors.invalidate();
	}

	private final SingleObjectCache<List<Integer>> 
	evaluationYearsCache = 
	new SingleObjectCache<>(
			new CacheLoader<List<Integer>>() {
				@Override
				public List<Integer> load() {
					List<Integer> evaluationYears = new ArrayList<>();
					try {
						for (Model m : triplestoreDAO.getSearchDAO().search(new SearchParams().type(Evaluation.IUCN_RED_LIST_EVALUATION_YEAR_CLASS))) {
							int year = Integer.valueOf(m.getStatements(Evaluation.EVALUATION_YEAR).get(0).getObjectLiteral().getContent());
							evaluationYears.add(year);
						}
					} catch (Exception e) {
						throw new RuntimeException("Evaluation years cache", e);
					}
					Collections.sort(evaluationYears);
					return evaluationYears;
				}
			}, 60*10);

	@Override
	public List<Integer> getEvaluationYears() throws Exception {
		return evaluationYearsCache.get();
	}

	@Override
	public Container getIUCNContainer() {
		return container;
	}

	private static final Object LOCK = new Object();

	public List<String> loadSpeciesOfGroup(String groupQname) throws Exception {
		HttpClientService client = null;
		try {
			client = new HttpClientService();
			return loadSpeciesOfGroup(groupQname, client);
		} finally {
			if (client !=  null) client.close();
		}
	}

	private List<String> loadSpeciesOfGroup(String groupQname, HttpClientService client) throws Exception {
		if (devMode && !DEV_LIMITED_TO_INFORMAL_GROUPS.contains(groupQname)) return Collections.emptyList();
		System.out.println("Loading species of group " + groupQname + " for IUCN evaluation...");
		List<String> speciesOfGroup = new ArrayList<>();
		synchronized (LOCK) { // To prevent too many requests at once
			URIBuilder uri = new URIBuilder(config.get("TaxonomyAPIURL") + "/" + BIOTA_QNAME + "/species")
					.addParameter(ONLY_FINNISH, true)
					.addParameter(INCLUDE_HIDDEN, true)
					.addParameter(SELECTED_FIELDS, QNAME)
					.addParameter(INFORMAL_GROUP_FILTERS, groupQname)
					.addParameter(PAGE, "1")
					.addParameter(PAGE_SIZE, PAGE_SIZE_TAXON_LIST);
			while (true) {
				JSONObject response = client.contentAsJson(new HttpGet(uri.getURI()));
				if (!response.hasKey(RESULTS)) throw new RuntimeException("Failed to get species: " + uri.toString());
				for (JSONObject species : response.getArray(RESULTS).iterateAsObject()) {
					speciesOfGroup.add(getQname(species));
				}
				if (response.hasKey(NEXT_PAGE)) {
					uri.replaceParameter(PAGE, response.getInteger(NEXT_PAGE));
				} else {
					break;
				}
			}
		}
		return speciesOfGroup;
	}

	@Override
	public List<String> getFinnishSpecies(String taxonQname) throws Exception {
		Taxon root = taxonomyDAO.getTaxon(new Qname(taxonQname));
		if (!root.hasChildren()) {
			if (root.isFinnishSpecies()) {
				return Utils.list(root.getQname().toString());
			}
			return Collections.emptyList();
		}

		HttpClientService client = null;
		try {
			client = new HttpClientService();
			List<String> species = getFinnishSpecies(taxonQname, client);
			if (root.isFinnishSpecies()) {
				species.add(root.getQname().toString());
			}
			return species;
		} finally {
			if (client != null) client.close();
		}
	}

	private List<String> getFinnishSpecies(String taxonQname, HttpClientService client) throws Exception {
		List<String> speciesOfTaxon = new ArrayList<>();
		synchronized (LOCK) { // To prevent too many request going out at once
			URIBuilder uri = new URIBuilder(config.get("TaxonomyAPIURL") + "/" + taxonQname + "/species")
					.addParameter(ONLY_FINNISH, true)
					.addParameter(SELECTED_FIELDS, QNAME)
					.addParameter(PAGE, 1)
					.addParameter(PAGE_SIZE, PAGE_SIZE_TAXON_LIST);
			while (true) {
				JSONObject response = client.contentAsJson(new HttpGet(uri.getURI()));
				if (!response.hasKey(RESULTS)) throw new RuntimeException("Failed to get species: " + uri.toString());
				for (JSONObject species : response.getArray(RESULTS).iterateAsObject()) {
					speciesOfTaxon.add(getQname(species));
				}
				if (response.hasKey(NEXT_PAGE)) {
					uri.replaceParameter(PAGE, response.getInteger(NEXT_PAGE));
				} else {
					break;
				}
			}

		}
		return speciesOfTaxon;
	}

	private String getQname(JSONObject taxon) {
		return taxon.getString(QNAME);
	}

	public EvaluationTarget loadTarget(String speciesQname) throws Exception {
		makeSureEvaluationDataIsLoaded();
		if (container.hasTarget(speciesQname)) return container.getTarget(speciesQname);
		return createTarget(speciesQname);
	}

	private EvaluationTarget createTarget(String speciesQname) throws Exception {
		EvaluationTarget target;
		target = new EvaluationTarget(new Qname(speciesQname), container, taxonomyDAO.getTaxonContainer());
		container.addTarget(target);
		return target;
	}

	public void makeSureEvaluationDataIsLoaded() throws Exception {
		if (!initialEvaluationsLoaded) {
			synchronized (EVAL_LOAD_LOCK) {
				if (!initialEvaluationsLoaded) {
					loadInitialEvaluations();
					initialEvaluationsLoaded = true;
				}
			}
		}		
	}

	private void loadInitialEvaluations() throws Exception {
		System.out.println("Loading IUCN evaluations...");

		SearchParams searchParams = new SearchParams(Integer.MAX_VALUE, 0).type(Evaluation.EVALUATION_CLASS);
		if (devMode) {
			limitLoadingForDevMode(searchParams);
		}
		Collection<Model> evaluations = triplestoreDAO.getSearchDAO().search(searchParams);

		for (Model model :  evaluations) {
			try {
				Evaluation evaluation = createEvaluation(model);
				String speciesQname = evaluation.getSpeciesQname();

				if (!container.hasTarget(speciesQname)) {
					createTarget(speciesQname);
				}
				EvaluationTarget target = container.getTarget(speciesQname);
				target.setEvaluation(evaluation);
			} catch (Exception e) {
				errorReporter.report("Error when loading evaluation " + model.getSubject().getQname(), e);
			}
		}
		System.out.println("IUCN evaluations loaded!");
	}

	private void limitLoadingForDevMode(SearchParams searchParams) throws Exception {
		for (String limitedGroupId : DEV_LIMITED_TO_INFORMAL_GROUPS) {
			for (String taxonId : loadSpeciesOfGroup(limitedGroupId)) {
				searchParams.objectresource(taxonId);
			}
		}
	}

	private Evaluation createEvaluation(Model model) throws Exception {
		Evaluation evaluation = new Evaluation(model, getEvaluationProperties());
		evaluation.setIncompletelyLoaded(true);
		return evaluation;
	}

	@Override
	public void completeLoading(Evaluation evaluation) throws Exception {
		setOccurrences(evaluation);
		setEndangermentReasons(evaluation);
		setHabitatObjects(evaluation);
		evaluation.setIncompletelyLoaded(false);
	}

	private void setHabitatObjects(Evaluation evaluation) throws Exception {
		Model model = evaluation.getModel();
		Set<Qname> habitatObjectIds = getHabitatObjectIds(model);
		Map<String, Model> asMap = getModelsAsMap(habitatObjectIds);

		if (model.hasStatements(Evaluation.PRIMARY_HABITAT)) {
			String id = getPrimaryHabitatId(model);
			Model habitatModel = asMap.get(id);
			if	(notGiven(habitatModel)) {
				errorReporter.report("Could not find primary habitat object " + id + " of " + evaluation.getId());
			} else {
				evaluation.setPrimaryHabitat(constructHabitatObject(habitatModel));
			}

		}
		for (Statement secondaryHabitat : model.getStatements(Evaluation.SECONDARY_HABITAT)) {
			Model habitatModel = asMap.get(secondaryHabitat.getObjectResource().getQname());
			if (notGiven(habitatModel)) {
				errorReporter.report("Could not find secondary habitat object " + secondaryHabitat.getObjectResource().getQname() + " of " + evaluation.getId());
				continue;
			}
			evaluation.addSecondaryHabitat(constructHabitatObject(habitatModel));
		}
	}

	private String getPrimaryHabitatId(Model model) {
		return model.getStatements(Evaluation.PRIMARY_HABITAT).get(0).getObjectResource().getQname();
	}

	public static HabitatObject constructHabitatObject(Model model) throws Exception {
		String habitat = model.getStatements(Evaluation.HABITAT).get(0).getObjectResource().getQname();
		int order = model.hasStatements(SORT_ORDER) ? Integer.valueOf(model.getStatements(SORT_ORDER).get(0).getObjectLiteral().getContent()) : 0;
		HabitatObject habitatObject = new HabitatObject(new Qname(model.getSubject().getQname()), new Qname(habitat), order);
		for (Statement type : model.getStatements(Evaluation.HABITAT_SPECIFIC_TYPE)) {
			habitatObject.addHabitatSpecificType(new Qname(type.getObjectResource().getQname()));
		}
		return habitatObject;
	}

	private Set<Qname> getHabitatObjectIds(Model model) {
		Set<Qname> habitatObjectIds = new HashSet<>();
		if (model.hasStatements(Evaluation.PRIMARY_HABITAT)) {
			habitatObjectIds.add(new Qname(getPrimaryHabitatId(model)));
		}
		for (Statement secondaryHabitat : model.getStatements(Evaluation.SECONDARY_HABITAT)) {
			habitatObjectIds.add(new Qname(secondaryHabitat.getObjectResource().getQname()));
		}
		return habitatObjectIds;
	}

	private void setEndangermentReasons(Evaluation evaluation) throws Exception {
		Model model = evaluation.getModel();
		Map<String, Model> modelsAsMap = getEndangermentModels(model);
		for (Statement s : model.getStatements(Evaluation.HAS_ENDANGERMENT_REASON)) {
			Model endangermentModel = modelsAsMap.get(s.getObjectResource().getQname());
			if (notGiven(endangermentModel)) {
				errorReporter.report("Could not find endangerment reason " + s.getObjectResource().getQname() + " of " + model.getSubject().getQname());
				continue;
			}
			EndangermentObject endangermentObject = getEndagermentObject(endangermentModel);
			evaluation.addEndangermentReason(endangermentObject);
		}
		for (Statement s : model.getStatements(Evaluation.HAS_THREAT)) {
			Model endangermentModel = modelsAsMap.get(s.getObjectResource().getQname());
			if (notGiven(endangermentModel)) {
				errorReporter.report("Could not find threat " + s.getObjectResource().getQname() + " of " + model.getSubject().getQname());
				continue;
			}
			EndangermentObject endangermentObject = getEndagermentObject(endangermentModel);
			evaluation.addThreat(endangermentObject);
		}
	}

	private boolean notGiven(Model model) {
		return model == null || model.isEmpty();
	}

	private Map<String, Model> getEndangermentModels(Model model) throws Exception {
		Set<Qname> endangermentObjectIds = getEndangermentIds(model);
		return getModelsAsMap(endangermentObjectIds);
	}

	private Set<Qname> getEndangermentIds(Model model) {
		Set<Qname> endangermentObjectIds = new HashSet<>();
		for (Statement s : model.getStatements(Evaluation.HAS_ENDANGERMENT_REASON)) {
			endangermentObjectIds.add(new Qname(s.getObjectResource().getQname()));
		}
		for (Statement s : model.getStatements(Evaluation.HAS_THREAT)) {
			endangermentObjectIds.add(new Qname(s.getObjectResource().getQname()));
		}
		return endangermentObjectIds;
	}

	private EndangermentObject getEndagermentObject(Model model) throws Exception {
		String endangerment = model.getStatements(Evaluation.ENDANGERMENT).get(0).getObjectResource().getQname();
		int order = model.hasStatements(SORT_ORDER) ? Integer.valueOf(model.getStatements(SORT_ORDER).get(0).getObjectLiteral().getContent()) : 0;
		EndangermentObject endangermentObject = new EndangermentObject(new Qname(model.getSubject().getQname()), new Qname(endangerment), order);
		return endangermentObject;
	}

	private void setOccurrences(Evaluation evaluation) throws Exception {
		Model model = evaluation.getModel();
		Map<String, Model> asMap = getOccurrenceModels(model);

		for (Statement hasOccurrence : model.getStatements(Evaluation.HAS_OCCURRENCE)) {
			Model occurrenceModel = asMap.get(hasOccurrence.getObjectResource().getQname());
			if (notGiven(occurrenceModel)) {
				errorReporter.report("Could not find occurrence " + hasOccurrence.getObjectResource().getQname() + " for evaluation " + evaluation.getId() + " of " + evaluation.getSpeciesQname());
				continue;
			}
			Occurrence occurrence = getOccurrence(occurrenceModel);
			evaluation.addOccurrence(occurrence);			
		}
	}

	private Map<String, Model> getOccurrenceModels(Model model) throws Exception {
		Set<Qname> occurrenceIds = getOccurrenceIds(model);
		return getModelsAsMap(occurrenceIds);
	}

	private Map<String, Model> getModelsAsMap(Set<Qname> subjects) throws Exception {
		if (subjects.isEmpty()) return Collections.emptyMap();
		Collection<Model> models = triplestoreDAO.getSearchDAO().get(subjects);
		return modelsAsMap(models);
	}

	private Map<String, Model> modelsAsMap(Collection<Model> models) {
		if (models.isEmpty()) return Collections.emptyMap();
		Map<String, Model> modelsAsMap = new HashMap<>();
		for (Model m : models) {
			modelsAsMap.put(m.getSubject().getQname(), m);
		}
		return modelsAsMap;
	}

	private Set<Qname> getOccurrenceIds(Model model) {
		Set<Qname> occurrenceIds = new HashSet<>();
		for (Statement hasOccurrence : model.getStatements(Evaluation.HAS_OCCURRENCE)) {
			occurrenceIds.add(new Qname(hasOccurrence.getObjectResource().getQname()));
		}
		return occurrenceIds;
	}

	private Occurrence getOccurrence(Model model) throws Exception {
		String areaQname = model.getStatements(MO_AREA).get(0).getObjectResource().getQname();
		String statusQname = model.getStatements(MO_STATUS).get(0).getObjectResource().getQname();
		Qname id = new Qname(model.getSubject().getQname());
		Occurrence occurrence = new Occurrence(id, new Qname(areaQname), new Qname(statusQname));
		if (model.hasStatements(MO_YEAR)) {
			int year = Integer.valueOf(model.getStatements(MO_YEAR).get(0).getObjectLiteral().getContent());
			occurrence.setYear(year);
		}
		if (model.hasStatements(MO_THREATENED)) {
			boolean threatened = TRUE.equals(model.getStatements(MO_THREATENED).get(0).getObjectLiteral().getContent());
			occurrence.setThreatened(threatened);
		}
		return occurrence;
	}

	private RdfProperties getEvaluationProperties() throws Exception {
		return triplestoreDAO.getProperties(Evaluation.EVALUATION_CLASS);
	}

	private final SingleObjectCache<Map<String, Area>> cachedEvaluationAreas = 
			new SingleObjectCache<Map<String, Area>>(
					new CacheLoader<Map<String, Area>>() {
						@Override
						public Map<String, Area> load() {
							Map<String, Area> areas = new LinkedHashMap<>();
							try {
								Collection<Model> models = triplestoreDAO.getSearchDAO().search(
										new SearchParams(100, 0)
										.type(AREA)
										.predicate(AREA_TYPE)
										.objectresource(EVALUATION_AREA_TYPE_QNAME.toString()));
								for (Model m : models) {
									String id = m.getSubject().getQname();
									LocalizedText name = new LocalizedText();
									for (Statement nameStatement : m.getStatements(ML_NAME)) {
										ObjectLiteral literal = nameStatement.getObjectLiteral();
										name.set(literal.getLangcode(), literal.getContent());
									}
									Area area = new Area(new Qname(id), name, EVALUATION_AREA_TYPE_QNAME);
									areas.put(id, area);
								}
							} catch (Exception e) {
								throw new RuntimeException("Loading evaluation areas", e);
							}
							return areas;
						}

					}, 60*60);

	@Override
	public Map<String, Area> getEvaluationAreas() throws Exception {
		return cachedEvaluationAreas.get();
	}

	@Override
	public Evaluation createNewEvaluation() throws Exception {
		Qname evaluationId = getSeqNextValAndAddResource();
		Model model = new Model(evaluationId);
		model.setType(Evaluation.EVALUATION_CLASS);
		return new Evaluation(model, getEvaluationProperties());
	}

	@Override
	public Evaluation createEvaluation(String id) throws Exception {
		Model model = new Model(new Qname(id));
		model.setType(Evaluation.EVALUATION_CLASS);
		return new Evaluation(model, getEvaluationProperties());
	}

	@Override
	public Qname getSeqNextValAndAddResource() throws Exception {
		return triplestoreDAO.getSeqNextValAndAddResource(Evaluation.IUCN_EVALUATION_NAMESPACE);
	}

	@Override
	public EditHistory getEditHistory(Evaluation thisPeriodData) throws Exception {
		EditHistory editHistory = new EditHistory();
		editHistory.add(buildEditHistory(thisPeriodData));
		getEditNotesFromHistory(thisPeriodData, editHistory);
		return editHistory;
	}

	private void getEditNotesFromHistory(Evaluation thisPeriodData, EditHistory editHistory) throws Exception, SQLException {
		TransactionConnection con = null;
		PreparedStatement p = null;
		ResultSet rs = null;
		try {
			con = triplestoreDAO.openConnection();
			p = con.prepareStatement(EDIT_HISTORY_SQL);
			p.setString(1, thisPeriodData.getId());
			rs = p.executeQuery();
			while (rs.next()) {
				String notes = rs.getString(1);
				String editorQname = rs.getString(2);
				editHistory.add(new EditHistoryEntry(notes, editorQname));
			}
		} finally {
			Utils.close(p, rs, con);
		}
	}

	private EditHistoryEntry buildEditHistory(Evaluation thisPeriodData) {
		return new EditHistoryEntry(thisPeriodData.getValue(Evaluation.EDIT_NOTES), thisPeriodData.getValue(Evaluation.LAST_MODIFIED_BY));
	}

	@Override
	public void moveEvaluation(String fromTaxonId, String toTaxonId, int year) throws Exception {
		EvaluationTarget from = getIUCNContainer().getTarget(fromTaxonId);
		EvaluationTarget to = getIUCNContainer().getTarget(toTaxonId);
		if (!from.hasEvaluation(year)) throw new IllegalStateException("From does not have evaluation for year " + year);
		if (to.hasEvaluation(year))  throw new IllegalStateException("To already has evaluation for year " + year);

		Evaluation evaluation = from.getEvaluation(year);
		Model model = evaluation.getModel();

		Predicate evaluatedTaxonPredicate = new Predicate(Evaluation.EVALUATED_TAXON); 
		model.removeAll(evaluatedTaxonPredicate);
		Statement s = new Statement(evaluatedTaxonPredicate, new ObjectResource(toTaxonId));
		model.addStatement(s);
		triplestoreDAO.store(new Subject(evaluation.getId()), s);

		getIUCNContainer().moveEvaluation(evaluation, from, to);
	}

	@Override
	public void deleteEvaluation(String fromTaxonId, int year) throws Exception {
		EvaluationTarget from = getIUCNContainer().getTarget(fromTaxonId);
		if (!from.hasEvaluation(year)) throw new IllegalStateException("From does not have evaluation for year " + year);

		Evaluation evaluation = from.getEvaluation(year);
		triplestoreDAO.delete(evaluation.getModel().getSubject());

		getIUCNContainer().deleteEvaluation(evaluation, from);
	}

}