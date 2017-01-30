package fi.luomus.triplestore.taxonomy.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import fi.luomus.commons.taxonomy.TaxonomyDAO;
import fi.luomus.commons.utils.SingleObjectCache;
import fi.luomus.commons.utils.SingleObjectCache.CacheLoader;
import fi.luomus.commons.utils.URIBuilder;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.dao.SearchParams;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.dao.TriplestoreDAOConst;
import fi.luomus.triplestore.taxonomy.iucn.model.EditHistory;
import fi.luomus.triplestore.taxonomy.iucn.model.EditHistory.EditHistoryEntry;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNContainer;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEditors;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEndangermentObject;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluation;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluationTarget;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNHabitatObject;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNRegionalStatus;

public class IucnDAOImple implements IucnDAO {

	private static final String SORT_ORDER = "sortOrder";
	private static final Predicate SORT_ORDER_PREDICATE = new Predicate(SORT_ORDER);
	private static final String MO_STATUS = "MO.status";
	private static final String MO_AREA = "MO.area";
	private static final String MO_YEAR = "MO.year";

	private static final String SCHEMA = TriplestoreDAOConst.SCHEMA;

	private static final String EDIT_HISTORY_SQL = "" + 
			" SELECT 	notesliteral.RESOURCELITERAL, userqname.RESOURCENAME " + 
			" FROM 		"+SCHEMA+".rdf_statement_history notes " + 
			" JOIN		"+SCHEMA+".rdf_resource notesliteral ON (notes.objectfk = notesliteral.resourceid) " + 
			" JOIN		"+SCHEMA+".rdf_resource userqname ON (notes.userfk = userqname.resourceid) " + 
			" WHERE		notes.SUBJECTFK = (select resourceid from "+SCHEMA+".rdf_resource where resourcename = ?) " + 
			" AND		notes.predicatefk = (select resourceid from "+SCHEMA+".rdf_resource where resourcename = '"+IUCNEvaluation.EDIT_NOTES+"') " + 
			" ORDER BY	notes.created DESC ";

	private static final String ML_NAME = "ML.name";
	private static final Qname EVALUATION_AREA_TYPE_QNAME = new Qname("ML.iucnEvaluationArea");
	private static final String AREA_TYPE = "ML.areaType";
	private static final String AREA = "ML.area";
	private static final String BIOTA_QNAME = "MX.37600";
	private static final String FI = "fi";
	private static final String QNAME = "qname";
	private static final String RDF_TYPE = "rdf:type";
	private static final String ONLY_FINNISH = "onlyFinnish";
	private static final String SELECTED_FIELDS = "selectedFields";
	private static final String PAGE_SIZE = "pageSize";
	private static final String PAGE = "page";
	private static final String NEXT_PAGE = "nextPage";
	private static final String RESULTS = "results";

	private final Config config;
	private final TriplestoreDAO triplestoreDAO;
	private final TaxonomyDAO taxonomyDAO;
	private final IUCNContainer container;
	private final ErrorReporter errorReporter;

	public IucnDAOImple(Config config, TriplestoreDAO triplestoreDAO, TaxonomyDAO taxonomyDAO, ErrorReporter errorReporter) {
		this.config = config;
		this.triplestoreDAO = triplestoreDAO;
		this.taxonomyDAO = taxonomyDAO;
		this.container = new IUCNContainer(this);
		this.errorReporter = errorReporter;
	}

	private final SingleObjectCache<Map<String, IUCNEditors>> 
	cachedGroupEditors = 
	new SingleObjectCache<>(
			new CacheLoader<Map<String, IUCNEditors>>() {
				@Override
				public Map<String, IUCNEditors> load() {
					try {
						Map<String, IUCNEditors> map = new HashMap<>();
						for (Model m : triplestoreDAO.getSearchDAO().search(RDF_TYPE, "MKV.taxonGroupIucnEditors")) {
							IUCNEditors editors = new IUCNEditors(new Qname(m.getSubject().getQname()));
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
	public Map<String, IUCNEditors> getGroupEditors() throws Exception {
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
						for (Model m : triplestoreDAO.getSearchDAO().search(RDF_TYPE, IUCNEvaluation.IUCN_RED_LIST_EVALUATION_YEAR_CLASS)) {
							int year = Integer.valueOf(m.getStatements(IUCNEvaluation.EVALUATION_YEAR).get(0).getObjectLiteral().getContent());
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
	public IUCNContainer getIUCNContainer() {
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
		List<String> speciesOfGroup = new ArrayList<>();
		synchronized (LOCK) { // To prevent too many requests at once
			URIBuilder uri = new URIBuilder(config.get("TaxonomyAPIURL") + "/" + BIOTA_QNAME + "/species")
					.addParameter(ONLY_FINNISH, true)
					.addParameter(SELECTED_FIELDS, "qname")
					.addParameter("informalGroupFilters", groupQname)
					.addParameter(PAGE, "1")
					.addParameter(PAGE_SIZE, "1000");
			System.out.println("Loading finnish species for informal group " + groupQname + " -> " + uri);
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
					.addParameter(SELECTED_FIELDS, "qname")
					.addParameter(PAGE, "1")
					.addParameter(PAGE_SIZE, "1000");
			System.out.println("Loading finnish species for " + taxonQname + " -> " + uri);
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

	public IUCNEvaluationTarget loadTarget(String speciesQname) throws Exception {
		Taxon taxon = taxonomyDAO.getTaxon(new Qname(speciesQname));
		IUCNEvaluationTarget target = new IUCNEvaluationTarget(speciesQname, taxon.getScientificName(), taxon.getVernacularName().forLocale(FI), container);
		for (IUCNEvaluation evaluation : getEvaluations(speciesQname)) {
			target.setEvaluation(evaluation);
		}
		return target;
	}

	private final Map<String, Collection<IUCNEvaluation>> initialEvaluations = new HashMap<>();

	private Collection<IUCNEvaluation> getEvaluations(String speciesQname) throws Exception {
		if (initialEvaluations.isEmpty()) {
			synchronized (initialEvaluations) {
				if (initialEvaluations.isEmpty()) {
					loadInitialEvaluations();
				}
			}
		}
		if (initialEvaluations.containsKey(speciesQname)) return initialEvaluations.get(speciesQname);
		return Collections.emptyList();
	}

	private void loadInitialEvaluations() throws Exception {
		System.out.println("Loading IUCN evaluations...");
		Collection<Model> evaluations = triplestoreDAO.getSearchDAO().search(new SearchParams(Integer.MAX_VALUE, 0).type(IUCNEvaluation.EVALUATION_CLASS));
		for (Model model :  evaluations) {
			try {
				IUCNEvaluation evaluation = createEvaluation(model);
				String speciesQname = evaluation.getSpeciesQname();
				if (!initialEvaluations.containsKey(speciesQname)) {
					initialEvaluations.put(speciesQname, new ArrayList<IUCNEvaluation>());
				}
				initialEvaluations.get(speciesQname).add(evaluation);
			} catch (Exception e) {
				errorReporter.report("Error when loading evaluation " + model.getSubject().getQname(), e);
			}
		}
		System.out.println("IUCN evaluations loaded!");
	}

	private IUCNEvaluation createEvaluation(Model model) throws Exception {
		IUCNEvaluation evaluation = new IUCNEvaluation(model, getEvaluationProperties());
		evaluation.setIncompletelyLoaded(true);
		return evaluation;
	}

	@Override
	public void completeLoading(IUCNEvaluation evaluation) throws Exception {
		System.out.println("Completing evaluation loading for " + evaluation.getSpeciesQname());
		Model model = evaluation.getModel();
		setOccurrences(model, evaluation);
		setRegionalStatuses(model, evaluation);
		setEndagermentReasons(model, evaluation);
		setPrimaryHabitat(model, evaluation);
		setSecondaryHabitats(model, evaluation);
		evaluation.setIncompletelyLoaded(false);
	}

	private void setEndagermentReasons(Model model, IUCNEvaluation evaluation) throws Exception {
		for (Statement endagermentReasonObject : model.getStatements(IUCNEvaluation.HAS_ENDANGERMENT_REASON)) {
			IUCNEndangermentObject endangermentObject = getEndagermentObject(new Qname(endagermentReasonObject.getObjectResource().getQname()));
			evaluation.addEndangermentReason(endangermentObject);
		}
		for (Statement endagermentReasonObject : model.getStatements(IUCNEvaluation.HAS_THREAT)) {
			IUCNEndangermentObject endangermentObject = getEndagermentObject(new Qname(endagermentReasonObject.getObjectResource().getQname()));
			evaluation.addThreat(endangermentObject);
		}
	}

	private IUCNEndangermentObject getEndagermentObject(Qname endagermentObjectId) throws Exception {
		Model model = triplestoreDAO.get(endagermentObjectId);
		String endangerment = model.getStatements(IUCNEvaluation.ENDANGERMENT).get(0).getObjectResource().getQname();
		int order = model.hasStatements(SORT_ORDER) ? Integer.valueOf(model.getStatements(SORT_ORDER).get(0).getObjectLiteral().getContent()) : 0;
		IUCNEndangermentObject endangermentObject = new IUCNEndangermentObject(endagermentObjectId, new Qname(endangerment), order);
		return endangermentObject;
	}

	private void setSecondaryHabitats(Model model, IUCNEvaluation evaluation) throws Exception {
		for (Statement secondaryHabitat : model.getStatements(IUCNEvaluation.SECONDARY_HABITAT)) {
			IUCNHabitatObject habitatObject = getHabitatObject(new Qname(secondaryHabitat.getObjectResource().getQname()));
			evaluation.addSecondaryHabitat(habitatObject);
		}
	}

	private void setPrimaryHabitat(Model model, IUCNEvaluation evaluation) throws Exception {
		if (model.hasStatements(IUCNEvaluation.PRIMARY_HABITAT)) {
			evaluation.setPrimaryHabitat(getHabitatObject(new Qname(model.getStatements(IUCNEvaluation.PRIMARY_HABITAT).get(0).getObjectResource().getQname())));
		}
	}

	private void setOccurrences(Model model, IUCNEvaluation evaluation) throws Exception {
		for (Statement hasOccurrence : model.getStatements(IUCNEvaluation.HAS_OCCURRENCE)) {
			evaluation.addOccurrence(getOccurrence(hasOccurrence.getObjectResource().getQname()));
		}
	}

	private void setRegionalStatuses(Model model, IUCNEvaluation evaluation) throws Exception {
		for (Statement hasRegionalStatus : model.getStatements(IUCNEvaluation.HAS_REGIONAL_STATUS)) {
			evaluation.addRegionalStatus(getRegionalStatus(hasRegionalStatus.getObjectResource().getQname()));
		}
	}

	private IUCNHabitatObject getHabitatObject(Qname habitatObjectId) throws Exception {
		Model model = triplestoreDAO.get(habitatObjectId);
		String habitat = model.getStatements(IUCNEvaluation.HABITAT).get(0).getObjectResource().getQname();
		int order = model.hasStatements(SORT_ORDER) ? Integer.valueOf(model.getStatements(SORT_ORDER).get(0).getObjectLiteral().getContent()) : 0;
		IUCNHabitatObject habitatObject = new IUCNHabitatObject(habitatObjectId, new Qname(habitat), order);
		for (Statement type : model.getStatements(IUCNEvaluation.HABITAT_SPECIFIC_TYPE)) {
			habitatObject.addHabitatSpecificType(new Qname(type.getObjectResource().getQname()));
		}
		return habitatObject;
	}

	private Occurrence getOccurrence(String occurrenceId) throws Exception {
		Model model = triplestoreDAO.get(occurrenceId);
		String areaQname = model.getStatements(MO_AREA).get(0).getObjectResource().getQname();
		String statusQname = model.getStatements(MO_STATUS).get(0).getObjectResource().getQname();
		Qname id = new Qname(model.getSubject().getQname());
		Occurrence occurrence = new Occurrence(id, new Qname(areaQname), new Qname(statusQname));
		if (model.hasStatements(MO_YEAR)) {
			int year = Integer.valueOf(model.getStatements(MO_YEAR).get(0).getObjectLiteral().getContent());
			occurrence.setYear(year);
		}
		return occurrence;
	}

	private IUCNRegionalStatus getRegionalStatus(String regionalStatusId) throws Exception {
		Model model = triplestoreDAO.get(regionalStatusId);
		String areaQname = model.getStatements(IUCNEvaluation.REGIONAL_STATUS_AREA).get(0).getObjectResource().getQname();
		String status = model.getStatements(IUCNEvaluation.REGIONAL_STATUS_STATUS).get(0).getObjectLiteral().getContent();
		Qname id = new Qname(model.getSubject().getQname());
		return new IUCNRegionalStatus(id, new Qname(areaQname), "true".equals(status));
	}

	private RdfProperties getEvaluationProperties() throws Exception {
		return triplestoreDAO.getProperties(IUCNEvaluation.EVALUATION_CLASS);
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
									Area area = new Area(new Qname(id), name, null, EVALUATION_AREA_TYPE_QNAME);
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
	public IUCNEvaluation createNewEvaluation() throws Exception {
		Qname evaluationId = getSeqNextValAndAddResource();
		Model model = new Model(evaluationId);
		model.setType(IUCNEvaluation.EVALUATION_CLASS);
		return new IUCNEvaluation(model, getEvaluationProperties());
	}

	@Override
	public IUCNEvaluation createEvaluation(String id) throws Exception {
		Model model = new Model(new Qname(id));
		model.setType(IUCNEvaluation.EVALUATION_CLASS);
		return new IUCNEvaluation(model, getEvaluationProperties());
	}

	@Override
	public Qname getSeqNextValAndAddResource() throws Exception {
		return triplestoreDAO.getSeqNextValAndAddResource(IUCNEvaluation.IUCN_EVALUATION_NAMESPACE);
	}

	private void store(IUCNHabitatObject habitat) throws Exception {
		Qname id = given(habitat.getId()) ? habitat.getId() : getSeqNextValAndAddResource();
		habitat.setId(id);
		Model model = new Model(new Subject(id));
		model.setType(IUCNEvaluation.HABITAT_OBJECT_CLASS);
		model.addStatement(new Statement(HABITAT_PREDICATE, new ObjectResource(habitat.getHabitat())));
		for (Qname type : habitat.getHabitatSpecificTypes()) {
			model.addStatement(new Statement(HABITAT_SPESIFIC_TYPE_PREDICATE, new ObjectResource(type)));
		}
		model.addStatement(new Statement(SORT_ORDER_PREDICATE, new ObjectLiteral(String.valueOf(habitat.getOrder()))));
		triplestoreDAO.store(model);
	}

	private void store(IUCNRegionalStatus regionalStatus) throws Exception {
		Qname id = given(regionalStatus.getId()) ? regionalStatus.getId() : getSeqNextValAndAddResource(); 
		regionalStatus.setId(id);
		Model model = new Model(id);
		model.setType(IUCNEvaluation.REGIONAL_STATUS_CLASS);
		model.addStatementIfObjectGiven(IUCNEvaluation.REGIONAL_STATUS_AREA, regionalStatus.getArea());
		model.addStatementIfObjectGiven(IUCNEvaluation.REGIONAL_STATUS_STATUS, regionalStatus.getStatus());
		triplestoreDAO.store(model);
	}

	private void store(IUCNEndangermentObject endangermentObject) throws Exception {
		Qname id = given(endangermentObject.getId()) ? endangermentObject.getId() : getSeqNextValAndAddResource();
		endangermentObject.setId(id);
		Model model = new Model(id);
		model.setType(IUCNEvaluation.ENDANGERMENT_OBJECT_CLASS);
		model.addStatementIfObjectGiven(IUCNEvaluation.ENDANGERMENT, endangermentObject.getEndangerment());
		model.addStatementIfObjectGiven(SORT_ORDER, String.valueOf(endangermentObject.getOrder()));
		triplestoreDAO.store(model);
	}

	private boolean given(Qname id) {
		return id != null && id.isSet();
	}

	@Override
	public EditHistory getEditHistory(IUCNEvaluation thisPeriodData) throws Exception {
		EditHistory editHistory = new EditHistory();
		editHistory.add(buildEditHistory(thisPeriodData));
		getEditNotesFromHistory(thisPeriodData, editHistory);
		return editHistory;
	}

	private void getEditNotesFromHistory(IUCNEvaluation thisPeriodData, EditHistory editHistory) throws Exception, SQLException {
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

	private EditHistoryEntry buildEditHistory(IUCNEvaluation thisPeriodData) {
		return new EditHistoryEntry(thisPeriodData.getValue(IUCNEvaluation.EDIT_NOTES), thisPeriodData.getValue(IUCNEvaluation.LAST_MODIFIED_BY));
	}

	@Override
	public void store(IUCNEvaluation givenData, IUCNEvaluation existingEvaluation) throws Exception {
		if (existingEvaluation != null) {
			deleteOccurrences(existingEvaluation);
			deleteEndangermentObjects(existingEvaluation);
			deleteHabitatObjects(existingEvaluation);
			deleteRegionalStatuses(existingEvaluation);
		}
		storeOccurrencesAndSetIdToModel(givenData);
		storeEndangermentObjectsAdnSetIdToModel(givenData);
		storeHabitatObjectsAndSetIdsToModel(givenData);
		storeRegionalStatusesAndSetIdToModel(givenData);
		triplestoreDAO.store(givenData.getModel());
	}

	private void storeEndangermentObjectsAdnSetIdToModel(IUCNEvaluation givenData) throws Exception {
		Model model = givenData.getModel();
		for (IUCNEndangermentObject endangermentObject : givenData.getEndangermentReasons()) {
			this.store(endangermentObject);
			model.addStatement(new Statement(HAS_ENDANGERMENT_REASON_PREDICATE, new ObjectResource(endangermentObject.getId())));
		}
		for (IUCNEndangermentObject endangermentObject : givenData.getThreats()) {
			this.store(endangermentObject);
			model.addStatement(new Statement(HAS_THREATH_PREDICATE, new ObjectResource(endangermentObject.getId())));
		}
	}

	private void storeHabitatObjectsAndSetIdsToModel(IUCNEvaluation givenData) throws Exception {
		Model model = givenData.getModel();
		IUCNHabitatObject primaryHabitat = givenData.getPrimaryHabitat();
		if (primaryHabitat != null) {
			this.store(primaryHabitat);
			model.addStatement(new Statement(PRIMARY_HABITAT_PREDICATE, new ObjectResource(primaryHabitat.getId())));
		}
		for (IUCNHabitatObject secondaryHabitat : givenData.getSecondaryHabitats()) {
			this.store(secondaryHabitat);
			model.addStatement(new Statement(SECONDARY_HABITAT_PREDICATE, new ObjectResource(secondaryHabitat.getId())));
		}
	}

	private void storeOccurrencesAndSetIdToModel(IUCNEvaluation givenData) throws Exception {
		for (Occurrence occurrence : givenData.getOccurrences()) {
			triplestoreDAO.store(new Qname(givenData.getSpeciesQname()), occurrence);
			givenData.getModel().addStatement(new Statement(HAS_OCCURRENCE_PREDICATE, new ObjectResource(occurrence.getId())));
		}
	}

	private void storeRegionalStatusesAndSetIdToModel(IUCNEvaluation givenData) throws Exception {
		for (IUCNRegionalStatus status : givenData.getRegionalStatuses()) {
			this.store(status);
			givenData.getModel().addStatement(new Statement(HAS_REGIONAL_STATUS_PREDICATE, new ObjectResource(status.getId())));
		}
	}

	private void deleteHabitatObjects(IUCNEvaluation existingEvaluation) throws Exception {
		if (existingEvaluation.getPrimaryHabitat() != null) {
			triplestoreDAO.delete(new Subject(existingEvaluation.getPrimaryHabitat().getId()));
		}
		for (IUCNHabitatObject habitat : existingEvaluation.getSecondaryHabitats()) {
			triplestoreDAO.delete(new Subject(habitat.getId()));
		}
	}

	private void deleteOccurrences(IUCNEvaluation existingEvaluation) throws Exception {
		for (Occurrence occurrence : existingEvaluation.getOccurrences()) {
			triplestoreDAO.delete(new Subject(occurrence.getId()));
		}
	}

	private void deleteEndangermentObjects(IUCNEvaluation existingEvaluation) throws Exception {
		for (IUCNEndangermentObject endangermentObject : existingEvaluation.getEndangermentReasons()) {
			triplestoreDAO.delete(new Subject(endangermentObject.getId()));
		}
		for (IUCNEndangermentObject endangermentObject : existingEvaluation.getThreats()) {
			triplestoreDAO.delete(new Subject(endangermentObject.getId()));
		}
	}

	private void deleteRegionalStatuses(IUCNEvaluation existingEvaluation) throws Exception {
		for (IUCNRegionalStatus status : existingEvaluation.getRegionalStatuses()) {
			triplestoreDAO.delete(new Subject(status.getId()));
		}
	}

}