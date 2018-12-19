package fi.luomus.triplestore.taxonomy.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.methods.HttpGet;
import org.apache.tomcat.jdbc.pool.DataSource;

import fi.luomus.commons.config.Config;
import fi.luomus.commons.containers.Area;
import fi.luomus.commons.containers.InformalTaxonGroup;
import fi.luomus.commons.containers.RedListEvaluationGroup;
import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.ObjectResource;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.RdfResource;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.db.connectivity.TransactionConnection;
import fi.luomus.commons.http.HttpClientService;
import fi.luomus.commons.json.JSONObject;
import fi.luomus.commons.reporting.ErrorReporter;
import fi.luomus.commons.taxonomy.Occurrences.Occurrence;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.taxonomy.TaxonSearch;
import fi.luomus.commons.taxonomy.TaxonSearchDAOSQLQueryImple;
import fi.luomus.commons.taxonomy.TaxonSearchDataSourceDefinition;
import fi.luomus.commons.taxonomy.TaxonSearchResponse;
import fi.luomus.commons.taxonomy.TaxonomyDAOBaseImple;
import fi.luomus.commons.taxonomy.iucn.Evaluation;
import fi.luomus.commons.taxonomy.iucn.HabitatObject;
import fi.luomus.commons.utils.Cached;
import fi.luomus.commons.utils.SingleObjectCache;
import fi.luomus.commons.utils.SingleObjectCache.CacheLoader;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.dao.SearchParams;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.dao.TriplestoreDAOConst;
import fi.luomus.triplestore.models.UsedAndGivenStatements;
import fi.luomus.triplestore.taxonomy.iucn.model.Container;
import fi.luomus.triplestore.taxonomy.iucn.model.EvaluationTarget;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

public class ExtendedTaxonomyDAOImple extends TaxonomyDAOBaseImple implements ExtendedTaxonomyDAO {

	private static final String SCHEMA = TriplestoreDAOConst.SCHEMA;
	private static final Predicate TYPE_OF_OCCURRENCE_IN_FINLAND_PREDICATE = new Predicate("MX.typeOfOccurrenceInFinland");

	private final TriplestoreDAO triplestoreDAO;
	private final CachedLiveLoadingTaxonContainer taxonContainer;
	private final Cached<TaxonSearch, TaxonSearchResponse> cachedTaxonSearches;
	private final Cached<Qname, Boolean> cachedDwUses;
	private final DataSource dataSource;
	private final Config config;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private final boolean devMode;
	private final ErrorReporter errorReporter;
	private IucnDAOImple iucnDAO;

	public ExtendedTaxonomyDAOImple(Config config, TriplestoreDAO triplestoreDAO, ErrorReporter errorReporter) {
		this(config, config.developmentMode(), triplestoreDAO, errorReporter);
	}

	public ExtendedTaxonomyDAOImple(Config config, boolean devMode, TriplestoreDAO triplestoreDAO, ErrorReporter errorReporter) {
		super(config, 3* 60 * 60, 10 * 60);
		System.out.println("Creating " +  ExtendedTaxonomyDAOImple.class.getName());
		this.dataSource = TaxonSearchDataSourceDefinition.initDataSource(config.connectionDescription());
		this.triplestoreDAO = triplestoreDAO;
		this.taxonContainer = new CachedLiveLoadingTaxonContainer(triplestoreDAO, this);
		this.iucnDAO = new IucnDAOImple(config, devMode, triplestoreDAO, taxonContainer, errorReporter);
		this.cachedTaxonSearches = new Cached<TaxonSearch, TaxonSearchResponse>(
				new TaxonSearchLoader(),
				60*60*3, 50000);
		this.cachedDwUses = new Cached<Qname, Boolean>(
				new DwUseLoader(),
				60*60*3, 50000);
		this.config = config;
		this.devMode = devMode;
		this.errorReporter = errorReporter;
		if (!devMode && config.productionMode()) {
			startNightlyScheduler();
		}
	}

	private void startNightlyScheduler() {
		int repeatPeriod24H = 24 * 60;
		long intitialDelay3am = calculateInitialDelayTill(3, repeatPeriod24H);
		long intitialDelay6am = calculateInitialDelayTill(6, repeatPeriod24H);

		scheduler.scheduleAtFixedRate(
				iucnDataToTaxonDataSynchronizer, 
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

	private final Runnable iucnContainerReinitializer = new Runnable() {
		@Override
		public void run() {
			iucnDAO = new IucnDAOImple(config, devMode, triplestoreDAO, taxonContainer, errorReporter); // TODO refactor method createIucnDAO
			try {
				for (String groupQname : iucnDAO.getGroupEditors().keySet()) {
					iucnDAO.getIUCNContainer().getTargetsOfGroup(groupQname);
				}
			} catch (Exception e) {
				errorReporter.report(e);
			}
		}
	};

	private final Runnable iucnRedListTaxonGroupNameUpdater = new Runnable() {
		@Override
		public void run() {
			System.out.println("Staring to update IUCN Red List Taxon Group names...");
			try {
				for (RedListEvaluationGroup group : getRedListEvaluationGroupsForceReload().values()) {
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

		private boolean namesDoNotMatch(RedListEvaluationGroup group, Model dbGroup) {
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

	private final Runnable iucnDataToTaxonDataSynchronizer = new Runnable() {
		@Override
		public void run() {
			System.out.println("Starting to synchronize taxon data with IUCN data...");
			try {
				Container container = iucnDAO.getIUCNContainer();
				container.makeSureEvaluationDataIsLoaded();
				int c = 1;
				for (EvaluationTarget target : container.getTargets()) {
					if (target.getQname() == null || target.getQname().isEmpty()) {
						errorReporter.report("Syncing taxon data with IUCN data: Null target taxon qname: " + debug(target));
						continue;
					}
					Qname speciesQname = new Qname(target.getQname());
					if (!getTaxonContainer().hasTaxon(speciesQname)) {
						errorReporter.report("Syncing taxon data with IUCN data: Taxon not found: " + speciesQname + " for target " + target);
						continue;
					}
					EditableTaxon taxon = (EditableTaxon) getTaxon(speciesQname);
					if (c++ % 5000 == 0) System.out.println(" ... syncing " + (c-1));
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
			if (!taxon.getTypesOfOccurrenceInFinland().isEmpty()) return false; // Never override types already set to taxon

			Evaluation evaluation = getLatestReadyEvaluation(target);
			if (evaluation == null) return false;

			Qname typeOfOccurrenceInFinland = new Qname(evaluation.getValue(Evaluation.TYPE_OF_OCCURRENCE_IN_FINLAND));
			if (!given(typeOfOccurrenceInFinland)) return false;

			updateTypesOfOccurrenceInFinland(taxon, typeOfOccurrenceInFinland);
			return true;
		}

		private boolean syncHabitats(EvaluationTarget target, EditableTaxon taxon) throws Exception {
			Evaluation evaluation = getLatestReadyEvaluation(target); // TODO change to latest locked
			if (evaluation == null) return false;

			if (!evaluation.getModel().hasStatements(Evaluation.PRIMARY_HABITAT)) return false;

			if (taxon.getPrimaryHabitatId() != null && !isNewestPossible(evaluation)) {
				return false; // don't override existing taxon data with old evaluation data
			}

			addHabitats(taxon);
			if (evaluation.isIncompletelyLoaded()) {
				iucnDAO.completeLoading(evaluation);
			}

			String taxonHabitats = habitatComparisonString(taxon.getPrimaryHabitat(), taxon.getSecondaryHabitats());
			String evaluationHabitats = habitatComparisonString(evaluation.getPrimaryHabitat(), evaluation.getSecondaryHabitats());
			if (taxonHabitats.equals(evaluationHabitats)) return false;

			updateHabitats(taxon, evaluation.getPrimaryHabitat(), evaluation.getSecondaryHabitats());
			return true;
		}

		private String habitatComparisonString(HabitatObject primaryHabitat, List<HabitatObject> secondaryHabitats) {
			StringBuilder b = new StringBuilder();
			b.append(habitatComparisonString(primaryHabitat));
			b.append("[");
			for (HabitatObject h : secondaryHabitats) {
				b.append(habitatComparisonString(h));
			}
			b.append("]");
			return b.toString();
		}

		private String habitatComparisonString(HabitatObject h) {
			if (h == null) return ";";
			return ""+h.getHabitat()+h.getHabitatSpecificTypes()+";";
		}

		private boolean isNewestPossible(Evaluation evaluation) throws Exception {
			return evaluation.getEvaluationYear().equals(newestEvaluationYear());
		}

		private Integer newestEvaluationYear() throws Exception {
			return iucnDAO.getEvaluationYears().iterator().next();
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

	private class DwUseLoader implements Cached.CacheLoader<Qname, Boolean> {
		@Override
		public Boolean load(Qname taxonId) {
			HttpClientService client = null;
			try {
				String dwUri = config.get("DwURL");
				client = new HttpClientService();
				JSONObject response = client.contentAsJson(new HttpGet(dwUri+"/"+taxonId.toString()));
				if (response.hasKey("used")) {
					return response.getBoolean("used");
				}
				if (response.hasKey("status")) {
					throw new Exception(dwUri + " returned status: " + response.getInteger("status") + " with message: " + response.getString("message"));
				}
				throw new Exception(dwUri + " returned unknown response");
			} catch (Exception e) {
				throw new RuntimeException("Dw use for " + taxonId, e);
			} finally {
				client.close();
			}
		}
	}

	private class TaxonSearchLoader implements Cached.CacheLoader<TaxonSearch, TaxonSearchResponse> {
		@Override
		public TaxonSearchResponse load(TaxonSearch key) {
			try {
				return uncachedTaxonSearch(key);
			} catch (Exception e) {
				throw new RuntimeException("Taxon search with terms " + key.toString(), e);
			}
		}
	}

	public void close() {
		try {
			if (scheduler != null) {
				scheduler.shutdownNow();
			}
		} catch (Exception e) {}
		if (dataSource != null) dataSource.close();
	}

	@Override
	public void clearCaches() {
		super.clearCaches();
		taxonContainer.clearCaches();
		cachedTaxonSearches.invalidateAll();
		cachedDwUses.invalidateAll();
	}

	@Override
	public EditableTaxon getTaxon(Qname qname) {
		return taxonContainer.getTaxon(qname);
	}

	@Override
	public void addHabitats(EditableTaxon taxon) {
		try {
			if (taxon.getPrimaryHabitat() != null) return;
			Set<String> habitatObjectIds = taxon.getHabitatIds();
			if (habitatObjectIds.isEmpty()) return;

			Collection<Model> models = triplestoreDAO.getSearchDAO().search(new SearchParams(1000, 0).subjects(habitatObjectIds));
			for (Model model : models) {
				HabitatObject h = IucnDAOImple.constructHabitatObject(model);
				if (h.getId().toString().equals(taxon.getPrimaryHabitatId())) {
					taxon.setPrimaryHabitat(h);
				} else {
					taxon.addSecondaryHabitat(h);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void addOccurrences(EditableTaxon taxon) {
		try {
			Collection<Model> models = triplestoreDAO.getSearchDAO().search(
					new SearchParams(1000, 0)
					.type("MO.occurrence")
					.predicate("MO.taxon")
					.objectresource(taxon.getQname().toString()));
			for (Model model : models) {
				Qname id = q(model.getSubject());
				Qname area = null;
				Qname status = null;
				String notes = null;
				Integer year = null;
				for (Statement s : model.getStatements()) {
					if (s.getPredicate().getQname().equals("MO.area")) {
						area = q(s.getObjectResource());
					} else if (s.getPredicate().getQname().equals("MO.status")) {
						status = q(s.getObjectResource());
					} else if (s.getPredicate().getQname().equals("MO.notes")) {
						notes = s.getObjectLiteral().getContent();
					} else if (s.getPredicate().getQname().equals("MO.year")) {
						try {
							year = Integer.valueOf(s.getObjectLiteral().getContent());
						} catch (Exception e) {}
					}
				}
				Occurrence occurrence = new Occurrence(id, area, status);
				occurrence.setNotes(notes);
				occurrence.setYear(year);
				taxon.getOccurrences().setOccurrence(occurrence);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Qname q(RdfResource resource) {
		return new Qname(resource.getQname());
	}


	private TaxonSearchResponse uncachedTaxonSearch(TaxonSearch taxonSearch) throws Exception {
		return new TaxonSearchDAOSQLQueryImple(this, dataSource, TriplestoreDAOConst.SCHEMA).search(taxonSearch);
	}

	@Override
	public TaxonSearchResponse search(TaxonSearch taxonSearch) throws Exception {
		return cachedTaxonSearches.get(taxonSearch);
	}

	@Override
	public IucnDAO getIucnDAO() {
		return iucnDAO;
	}

	@Override
	public CachedLiveLoadingTaxonContainer getTaxonContainer() throws Exception {
		return taxonContainer;
	}

	@Override
	public List<Taxon> taxonNameExistsInChecklistForOtherTaxon(String name, Taxon taxon) throws Exception {
		List<Taxon> matches = new ArrayList<Taxon>();

		Qname checklist = null;
		if (given(taxon.getChecklist())) {
			checklist = taxon.getChecklist();
		} else {
			Taxon synonymParent = taxon.getSynonymParent();
			if (synonymParent != null) {
				checklist = synonymParent.getChecklist();
			}
			if (!given(checklist)) {
				checklist = new Qname("MR.1");
			}
		}

		TransactionConnection con = null;
		PreparedStatement p = null;
		ResultSet rs = null;
		try {
			con = triplestoreDAO.openConnection();
			p = con.prepareStatement("" +
					" SELECT qname, scientificname, author, taxonrank FROM "+SCHEMA+".taxon_search_materialized " +
					" WHERE checklist = ? AND name = ? AND qname != ? ");
			p.setString(1, checklist.toString());
			p.setString(2, name.toUpperCase());
			p.setString(3, taxon.getQname().toString());
			rs = p.executeQuery();
			rs.setFetchSize(4001);
			while (rs.next()) {
				Qname matchQname = new Qname(rs.getString(1));
				String matchScientificName = rs.getString(2);
				String matchAuthor = rs.getString(3);
				String matchRank = rs.getString(4);
				Taxon match = new Taxon(matchQname, null);
				match.setScientificName(matchScientificName);
				match.setScientificNameAuthorship(matchAuthor);
				if (given(matchRank)) {
					match.setTaxonRank(new Qname(matchRank));
				}
				matches.add(match);
			}
		} finally {
			Utils.close(p, rs, con);
		}
		return matches;
	}

	private boolean given(Object o) {
		return o != null && o.toString().trim().length() > 0;
	}

	@Override
	public EditableTaxon createTaxon() throws Exception {
		Qname qname = triplestoreDAO.getSeqNextValAndAddResource("MX");
		return new EditableTaxon(qname, taxonContainer, this);
	}

	@Override
	public Set<String> getInformalTaxonGroupRoots() {
		return getRoots(getInformalTaxonGroups());
	}

	@Override
	public Set<String> getIucnRedListInformalGroupRoots() {
		return getRoots(getRedListEvaluationGroups());
	}

	private <T extends InformalTaxonGroup> Set<String> getRoots(Map<String, T> allGroups) {
		Set<String> roots = new LinkedHashSet<>(allGroups.keySet());
		for (T group : allGroups.values()) {
			for (Qname subGroup : group.getSubGroups()) {
				roots.remove(subGroup.toString());
			}
		}
		return roots;
	}

	private final SingleObjectCache<Map<String, Area>> cachedBiogeographicalProvinces = 
			new SingleObjectCache<Map<String, Area>>(
					new CacheLoader<Map<String, Area>>() {
						private final Qname BIOGEOGRAPHICAL_PROVINCE = new Qname("ML.biogeographicalProvince");
						@Override
						public Map<String, Area> load() {
							try {
								Map<String, Area> areas = new LinkedHashMap<>();
								for (Area area : getAreas().values()) {
									if (BIOGEOGRAPHICAL_PROVINCE.equals(area.getType())) {
										areas.put(area.getQname().toString(), area);
									}
								}
								return areas;
							} catch (Exception e) {
								throw new RuntimeException("Loading biogeographical provinces", e);
							}
						}
					}, 60*60*7);

	@Override
	public Map<String, Area> getBiogeographicalProvinces() throws Exception {
		return cachedBiogeographicalProvinces.get();
	}

	@Override
	public boolean isTaxonIdUsedInDataWarehouse(Qname taxonId) {
		return cachedDwUses.get(taxonId);
	}

}
