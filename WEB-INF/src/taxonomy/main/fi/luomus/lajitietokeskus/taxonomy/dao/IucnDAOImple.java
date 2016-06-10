package fi.luomus.lajitietokeskus.taxonomy.dao;

import fi.luomus.commons.config.Config;
import fi.luomus.commons.containers.Area;
import fi.luomus.commons.containers.LocalizedText;
import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.ObjectLiteral;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.http.HttpClientService;
import fi.luomus.commons.json.JSONObject;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.taxonomy.TaxonomyDAO;
import fi.luomus.commons.utils.SingleObjectCache;
import fi.luomus.commons.utils.SingleObjectCache.CacheLoader;
import fi.luomus.triplestore.dao.SearchParams;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.models.IUCNContainer;
import fi.luomus.triplestore.taxonomy.models.IUCNEditors;
import fi.luomus.triplestore.taxonomy.models.IUCNEvaluation;
import fi.luomus.triplestore.taxonomy.models.IUCNEvaluationTarget;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.client.methods.HttpGet;

public class IucnDAOImple implements IucnDAO {

	private static final String ML_NAME = "ML.name";
	private static final String IUCN_EVALUATION_AREA = "ML.iucnEvaluationArea";
	private static final Qname EVALUATION_AREA_TYPE_QNAME = new Qname(IUCN_EVALUATION_AREA);
	private static final String AREA_TYPE = "ML.areaType";
	private static final String AREA = "ML.area";
	private static final String IUCN_RED_LIST_EVALUATION = "MKV.iucnRedListEvaluation";
	private static final String FI = "fi";
	private static final String IS_SPECIES = "isSpecies";
	private static final String CHECKLIST = "checklist";
	private static final String QNAME = "qname";
	private static final String CHILDREN = "children";
	private static final String ROOT = "root";
	private static final String NAME_ACCORDING_TO = "MX.nameAccordingTo";
	private static final String IS_PART_OF_INFORMAL_TAXON_GROUP = "MX.isPartOfInformalTaxonGroup";
	private static final String EVALUATION_YEAR = "MKV.evaluationYear";
	private static final String IUCN_RED_LIST_EVALUATION_YEAR = "MKV.iucnRedListEvaluationYear";
	private static final String RDF_TYPE = "rdf:type";
	private static final String MASTER_CHECKLIST_QNAME = "MR.1";
	private final Config config;
	private final TriplestoreDAO triplestoreDAO;
	private final TaxonomyDAO taxonomyDAO;
	private final IUCNContainer container;

	public IucnDAOImple(Config config, TriplestoreDAO triplestoreDAO, TaxonomyDAO taxonomyDAO) {
		this.config = config;
		this.triplestoreDAO = triplestoreDAO;
		this.taxonomyDAO = taxonomyDAO;
		this.container = new IUCNContainer(triplestoreDAO, this);
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
							map.put(groupQname, editors);
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
						for (Model m : triplestoreDAO.getSearchDAO().search(RDF_TYPE, IUCN_RED_LIST_EVALUATION_YEAR)) {
							int year = Integer.valueOf(m.getStatements(EVALUATION_YEAR).get(0).getObjectLiteral().getContent());
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
		if (config.developmentMode() && !(groupQname.equals("MVL.27") || groupQname.equals("MVL.1") || groupQname.equals("MVL.26"))) return Collections.emptyList(); //XXX
		Set<String> rootTaxonsOfGroup = getRootTaxonsOfGroup(groupQname);
		List<String> speciesOfGroup = new ArrayList<>();
		HttpClientService client = null;
		try {
			client = new HttpClientService();
			for (String rootTaxonQname : rootTaxonsOfGroup) {
				addSpeciesOfTaxon(speciesOfGroup, client, rootTaxonQname);
			}
		} finally {
			if (client !=  null) client.close();
		}
		return speciesOfGroup;
	}

	private Set<String> getRootTaxonsOfGroup(String groupQname) throws Exception {
		SearchParams searchParams = new SearchParams(Integer.MAX_VALUE, 0).predicate(IS_PART_OF_INFORMAL_TAXON_GROUP).objectresource(groupQname);
		Set<String> rootTaxonsOfGroup = new HashSet<>();
		for (Model m : triplestoreDAO.getSearchDAO().search(searchParams)) {
			if (!m.hasStatements(NAME_ACCORDING_TO)) continue;
			String checklist = m.getStatements(NAME_ACCORDING_TO).get(0).getObjectResource().getQname();
			if (fromMasterChecklist(checklist)) {
				rootTaxonsOfGroup.add(m.getSubject().getQname());
			}
		}
		return rootTaxonsOfGroup;
	}

	private boolean fromMasterChecklist(String checklistQname) {
		return MASTER_CHECKLIST_QNAME.equals(checklistQname);
	}

	@Override
	public List<String> getFinnishSpecies(String taxonQname) throws Exception {
		List<String> list = new ArrayList<>();
		HttpClientService client = null;
		try {
			client = new HttpClientService();
			addSpeciesOfTaxon(list, client, taxonQname);
		} finally {
			if (client != null) client.close();
		}
		return list;
	}
	private void addSpeciesOfTaxon(List<String> speciesOfGroup, HttpClientService client, String rootTaxonQname) throws Exception {
		System.out.println("Loading finnish species for " + rootTaxonQname);
		synchronized (LOCK) {
			URI uri = new URI(config.get("TaxonomyAPIURL")+"/" + rootTaxonQname + "/finnish/species?selectedFields=qname,checklist,isSpecies");
			JSONObject response = client.contentAsJson(new HttpGet(uri));
			JSONObject root = response.getObject(ROOT);
			if (validSpecies(root)) {
				speciesOfGroup.add(getQname(root));
			}
			for (JSONObject species : response.getArray(CHILDREN).iterateAsObject()) {
				if (validSpecies(species)) {
					speciesOfGroup.add(getQname(species));
				}
			}
		}
	}

	private String getQname(JSONObject taxon) {
		return taxon.getObject(QNAME).getString(QNAME);
	}

	private boolean validSpecies(JSONObject taxon) {
		String checklistQname = taxon.getObject(CHECKLIST).getString(QNAME);
		boolean isSpecies = taxon.getBoolean(IS_SPECIES);
		return isSpecies && fromMasterChecklist(checklistQname);
	}

	public IUCNEvaluationTarget loadTarget(String speciesQname) throws Exception {
		Taxon taxon = taxonomyDAO.getTaxon(new Qname(speciesQname));
		IUCNEvaluationTarget target = new IUCNEvaluationTarget(speciesQname, taxon.getScientificName(), taxon.getVernacularName(FI), container);
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
		Collection<Model> evaluations = triplestoreDAO.getSearchDAO().search(new SearchParams(Integer.MAX_VALUE, 0).type(IUCN_RED_LIST_EVALUATION));
		for (Model model :  evaluations) {
			IUCNEvaluation evaluation = new IUCNEvaluation(model);
			String speciesQname = evaluation.getSpeciesQname();
			if (!initialEvaluations.containsKey(speciesQname)) {
				initialEvaluations.put(speciesQname, new ArrayList<IUCNEvaluation>());
			}
			initialEvaluations.get(speciesQname).add(evaluation);
		}
		System.out.println("IUCN evaluations loaded!");
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
										.objectresource(IUCN_EVALUATION_AREA));
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

}