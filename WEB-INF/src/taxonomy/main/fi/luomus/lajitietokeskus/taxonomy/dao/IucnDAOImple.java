package fi.luomus.lajitietokeskus.taxonomy.dao;

import fi.luomus.commons.config.Config;
import fi.luomus.commons.containers.rdf.Model;
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
import fi.luomus.triplestore.taxonomy.models.IUCNEvaluation;
import fi.luomus.triplestore.taxonomy.models.IUCNEvaluationTarget;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.client.methods.HttpGet;

public class IucnDAOImple implements IucnDAO {

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

	private final SingleObjectCache<Map<String, List<Qname>>> 
	cachedGroupEditors = 
	new SingleObjectCache<>(
			new CacheLoader<Map<String, List<Qname>>>() {
				@Override
				public Map<String, List<Qname>> load() {
					try {
						Map<String, List<Qname>> map = new HashMap<>();
						for (Model m : triplestoreDAO.getSearchDAO().search("rdf:type", "MKV.taxonGroupIucnEditors")) {
							String groupQname = m.getStatements("MKV.taxonGroup").get(0).getObjectResource().getQname();
							List<Qname> groupEditors = new ArrayList<>();
							for (Statement editor : m.getStatements("MKV.iucnEditor")) {
								groupEditors.add(new Qname(editor.getObjectResource().getQname()));
							}
							map.put(groupQname, groupEditors);
						}
						return map;
					} catch (Exception e) {
						throw new RuntimeException("Cached group editors", e);
					}
				}
			}, 5*60);

	@Override
	public Map<String, List<Qname>> getGroupEditors() throws Exception {
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
						for (Model m : triplestoreDAO.getSearchDAO().search("rdf:type", "MKV.iucnRedListEvaluationYear")) {
							int year = Integer.valueOf(m.getStatements("MKV.evaluationYear").get(0).getObjectLiteral().getContent());
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
		SearchParams searchParams = new SearchParams(Integer.MAX_VALUE, 0).predicate("MX.isPartOfInformalTaxonGroup").objectresource(groupQname);
		Set<String> rootTaxonsOfGroup = new HashSet<>();
		for (Model m : triplestoreDAO.getSearchDAO().search(searchParams)) {
			if (!m.hasStatements("MX.nameAccordingTo")) continue;
			String checklist = m.getStatements("MX.nameAccordingTo").get(0).getObjectResource().getQname();
			if (fromMasterChecklist(checklist)) {
				rootTaxonsOfGroup.add(m.getSubject().getQname());
			}
		}
		return rootTaxonsOfGroup;
	}

	private boolean fromMasterChecklist(String checklistQname) {
		return MASTER_CHECKLIST_QNAME.equals(checklistQname);
	}

	private void addSpeciesOfTaxon(List<String> speciesOfGroup, HttpClientService client, String rootTaxonQname) throws Exception {
		System.out.println("Loading finnish species for " + rootTaxonQname);
		synchronized (LOCK) {
			URI uri = new URI(config.get("TaxonomyAPIURL")+"/" + rootTaxonQname + "/finnish/species?selectedFields=qname,checklist,isSpecies");
			JSONObject response = client.contentAsJson(new HttpGet(uri));
			JSONObject root = response.getObject("root");
			if (validSpecies(root)) {
				speciesOfGroup.add(getQname(root));
			}
			for (JSONObject species : response.getArray("children").iterateAsObject()) {
				if (validSpecies(species)) {
					speciesOfGroup.add(getQname(species));
				}
			}
		}
	}

	private String getQname(JSONObject taxon) {
		return taxon.getObject("qname").getString("qname");
	}

	private boolean validSpecies(JSONObject taxon) {
		String checklistQname = taxon.getObject("checklist").getString("qname");
		boolean isSpecies = taxon.getBoolean("isSpecies");
		return isSpecies && fromMasterChecklist(checklistQname);
	}

	public IUCNEvaluationTarget loadTarget(String speciesQname) throws Exception {
		Taxon taxon = taxonomyDAO.getTaxon(new Qname(speciesQname));
		IUCNEvaluationTarget target = new IUCNEvaluationTarget(speciesQname, taxon.getScientificName(), taxon.getVernacularName("fi"), container);
		for (Model evaluation : getEvaluations(speciesQname)) {
			target.setEvaluation(new IUCNEvaluation(evaluation));
		}
		return target;
	}

	private Collection<Model> getEvaluations(String speciesQname) throws Exception {
		return triplestoreDAO.getSearchDAO().search(
				new SearchParams(Integer.MAX_VALUE, 0)
				.type("MKV.iucnRedListEvaluation")
				.predicate("MKV.evaluatedTaxon")
				.objectresource(speciesQname));
	}

}