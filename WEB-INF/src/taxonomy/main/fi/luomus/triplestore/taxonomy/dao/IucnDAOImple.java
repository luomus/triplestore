package fi.luomus.triplestore.taxonomy.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.client.methods.HttpGet;

import fi.luomus.commons.config.Config;
import fi.luomus.commons.containers.Area;
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
import fi.luomus.commons.taxonomy.TaxonContainer;
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
import fi.luomus.triplestore.taxonomy.iucn.model.Container;
import fi.luomus.triplestore.taxonomy.iucn.model.EditHistory;
import fi.luomus.triplestore.taxonomy.iucn.model.EditHistory.EditHistoryEntry;
import fi.luomus.triplestore.taxonomy.iucn.model.Editors;
import fi.luomus.triplestore.taxonomy.iucn.model.EvaluationTarget;
import fi.luomus.triplestore.taxonomy.iucn.model.EvaluationYear;

public class IucnDAOImple implements IucnDAO {

	private static final String TRUE = "true";
	private static final int PAGE_SIZE_TAXON_LIST = 30000;
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
	private static final String ID = "id";
	private static final String ONLY_FINNISH = "onlyFinnish";
	private static final String SELECTED_FIELDS = "selectedFields";
	private static final String PAGE_SIZE = "pageSize";
	private static final String PAGE = "page";
	private static final String NEXT_PAGE = "nextPage";
	private static final String RESULTS = "results";

	private static final Object EVAL_LOAD_LOCK = new Object();

	private final Config config;
	private final TriplestoreDAO triplestoreDAO;
	private final TaxonContainer taxonContainer;
	private Container container;
	private final ErrorReporter errorReporter;
	private final boolean devMode;
	private boolean initialEvaluationsLoaded = false;

	public IucnDAOImple(Config config, boolean devMode, TriplestoreDAO triplestoreDAO, TaxonContainer taxonContainer, ErrorReporter errorReporter) {
		System.out.println("Creating " +  IucnDAOImple.class.getName());
		this.config = config;
		this.devMode = devMode;
		this.triplestoreDAO = triplestoreDAO;
		this.taxonContainer = taxonContainer;
		this.container = new Container(this);
		this.errorReporter = errorReporter;
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

	private final SingleObjectCache<List<EvaluationYear>> 
	evaluationYearsCache = 
	new SingleObjectCache<>(
			new CacheLoader<List<EvaluationYear>>() {
				@Override
				public List<EvaluationYear> load() {
					List<EvaluationYear> evaluationYears = new ArrayList<>();
					try {
						for (Model m : triplestoreDAO.getSearchDAO().search(new SearchParams().type(Evaluation.IUCN_RED_LIST_EVALUATION_YEAR_CLASS))) {
							int year = Integer.valueOf(m.getStatements(Evaluation.EVALUATION_YEAR).get(0).getObjectLiteral().getContent());
							boolean locked = false;
							if (m.hasStatements(Evaluation.IS_LOCKED)) {
								locked = "true".equals(m.getStatements(Evaluation.IS_LOCKED).get(0).getObjectLiteral().getContent());
							}
							evaluationYears.add(new EvaluationYear(year, locked));
						}
					} catch (Exception e) {
						throw new RuntimeException("Evaluation years cache", e);
					}
					Collections.sort(evaluationYears, new Comparator<EvaluationYear>() {
						@Override
						public int compare(EvaluationYear o1, EvaluationYear o2) {
							return Integer.compare(o1.getYear(), o2.getYear());
						}
					});
					return evaluationYears;
				}
			}, 60*10);

	@Override
	public List<EvaluationYear> getEvaluationYears() throws Exception {
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
					.addParameter(SELECTED_FIELDS, ID)
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
		Taxon root = taxonContainer.getTaxon(new Qname(taxonQname));
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
					.addParameter(SELECTED_FIELDS, ID)
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
		return taxon.getString(ID);
	}

	public EvaluationTarget loadTarget(String speciesQname) throws Exception {
		makeSureEvaluationDataIsLoaded();
		if (container.hasTarget(speciesQname)) return container.getTarget(speciesQname);
		return createTarget(speciesQname);
	}

	private EvaluationTarget createTarget(String speciesQname) {
		EvaluationTarget target;
		target = new EvaluationTarget(new Qname(speciesQname), container, taxonContainer);
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

	@Override
	public Evaluation createEvaluation(Model model) throws Exception {
		Evaluation evaluation = new Evaluation(model, getEvaluationProperties());
		evaluation.setIncompletelyLoaded(true);
		return evaluation;
	}

	@Override
	public void completeLoading(Evaluation evaluation) throws Exception {
		Map<String, Model> attachedObjects = getAttachedObjectModels(evaluation);
		setOccurrences(evaluation, attachedObjects);
		setEndangermentReasons(evaluation, attachedObjects);
		setHabitatObjects(evaluation, attachedObjects);
		evaluation.setIncompletelyLoaded(false);
	}

	private Map<String, Model> getAttachedObjectModels(Evaluation evaluation) throws Exception {
		Model model = evaluation.getModel();
		Set<Qname> ids = getOccurrenceIds(model);
		ids.addAll(getEndangermentIds(model));
		ids.addAll(getHabitatObjectIds(model));
		Map<String, Model> asMap = getModelsAsMap(ids);
		return asMap;
	}

	private void setHabitatObjects(Evaluation evaluation, Map<String, Model> asMap) throws Exception {
		Model model = evaluation.getModel();
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

	private void setEndangermentReasons(Evaluation evaluation, Map<String, Model> attatchedObjects) throws Exception {
		Model model = evaluation.getModel();
		for (Statement s : model.getStatements(Evaluation.HAS_ENDANGERMENT_REASON)) {
			Model endangermentModel = attatchedObjects.get(s.getObjectResource().getQname());
			if (notGiven(endangermentModel)) {
				errorReporter.report("Could not find endangerment reason " + s.getObjectResource().getQname() + " of " + model.getSubject().getQname());
				continue;
			}
			EndangermentObject endangermentObject = getEndagermentObject(endangermentModel);
			evaluation.addEndangermentReason(endangermentObject);
		}
		for (Statement s : model.getStatements(Evaluation.HAS_THREAT)) {
			Model endangermentModel = attatchedObjects.get(s.getObjectResource().getQname());
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

	private void setOccurrences(Evaluation evaluation, Map<String, Model> attachedObjects) throws Exception {
		Model model = evaluation.getModel();
		for (Statement hasOccurrence : model.getStatements(Evaluation.HAS_OCCURRENCE)) {
			Model occurrenceModel = attachedObjects.get(hasOccurrence.getObjectResource().getQname());
			if (notGiven(occurrenceModel)) {
				errorReporter.report("Could not find occurrence " + hasOccurrence.getObjectResource().getQname() + " for evaluation " + evaluation.getId() + " of " + evaluation.getSpeciesQname());
				continue;
			}
			Occurrence occurrence = getOccurrence(occurrenceModel);
			evaluation.addOccurrence(occurrence);			
		}
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

	@Override
	public RdfProperties getEvaluationProperties() throws Exception {
		return triplestoreDAO.getProperties(Evaluation.EVALUATION_CLASS);
	}

	private final SingleObjectCache<Map<String, Area>> cachedEvaluationAreas = 
			new SingleObjectCache<>(
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