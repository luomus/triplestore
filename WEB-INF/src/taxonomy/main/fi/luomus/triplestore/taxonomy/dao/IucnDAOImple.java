package fi.luomus.triplestore.taxonomy.dao;

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
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluation;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluationTarget;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNHabitatObject;

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

public class IucnDAOImple implements IucnDAO {

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

	public IucnDAOImple(Config config, TriplestoreDAO triplestoreDAO, TaxonomyDAO taxonomyDAO) {
		this.config = config;
		this.triplestoreDAO = triplestoreDAO;
		this.taxonomyDAO = taxonomyDAO;
		this.container = new IUCNContainer(this);
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
			IUCNEvaluation evaluation = new IUCNEvaluation(model, getEvaluationProperties());
			String speciesQname = evaluation.getSpeciesQname();
			if (!initialEvaluations.containsKey(speciesQname)) {
				initialEvaluations.put(speciesQname, new ArrayList<IUCNEvaluation>());
			}
			for (Statement hasOccurrence : model.getStatements("MKV.hasOccurrence")) {
				evaluation.addOccurrence(getOccurrence(hasOccurrence.getObjectResource().getQname()));
			}
			if (model.hasStatements("MKV.primaryHabitat")) {
				evaluation.setPrimaryHabitat(getHabitatObject(model.getStatements("MKV.primaryHabitat").get(0).getObjectResource().getQname()));
			}
			for (Statement secondaryHabitat : model.getStatements("MKV.secondaryHabitat")) {
				evaluation.addSecondaryHabitat(getHabitatObject(secondaryHabitat.getObjectResource().getQname()));
			}
			initialEvaluations.get(speciesQname).add(evaluation);
		}
		System.out.println("IUCN evaluations loaded!");
	}

	private IUCNHabitatObject getHabitatObject(String habitatObjectId) throws Exception {
		Model model = triplestoreDAO.get(habitatObjectId);
		String habitat = model.getStatements("MKV.habitat").get(0).getObjectResource().getQname();
		IUCNHabitatObject habitatObject = new IUCNHabitatObject(habitatObjectId, habitat);
		for (Statement type : model.getStatements("MKV.habitatSpecificType")) {
			habitatObject.addHabitatSpecificType(type.getObjectResource().getQname());
		}
		return habitatObject;
	}

	private Occurrence getOccurrence(String occurrenceId) throws Exception {
		Model model = triplestoreDAO.get(occurrenceId);
		String areaQname = model.getStatements("MO.area").get(0).getObjectResource().getQname();
		String statusQname = model.getStatements("MO.status").get(0).getObjectResource().getQname();
		return new Occurrence(new Qname(model.getSubject().getQname()), new Qname(areaQname), new Qname(statusQname));
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

	@Override
	public void store(IUCNHabitatObject habitat) throws Exception {
		String id = given(habitat.getId()) ? habitat.getId() : getSeqNextValAndAddResource().toString();
		habitat.setId(id);
		Model model = new Model(new Subject(id));
		model.setType(IUCNEvaluation.HABITAT_OBJECT_CLASS);
		model.addStatement(new Statement(new Predicate(IUCNEvaluation.HABITAT), new ObjectResource(habitat.getHabitat())));
		for (String type : habitat.getHabitatSpecificTypes()) {
			model.addStatement(new Statement(new Predicate(IUCNEvaluation.HABITAT_SPECIFIC_TYPE), new ObjectResource(type)));
		}
		triplestoreDAO.store(model);
	}

	private boolean given(String id) {
		return id != null && id.length() > 0;
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

}