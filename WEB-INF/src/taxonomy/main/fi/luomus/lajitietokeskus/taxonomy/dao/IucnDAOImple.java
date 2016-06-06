package fi.luomus.lajitietokeskus.taxonomy.dao;

import fi.luomus.commons.config.Config;
import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.http.HttpClientService;
import fi.luomus.commons.json.JSONObject;
import fi.luomus.commons.utils.SingleObjectCacheResourceInjected;
import fi.luomus.commons.utils.SingleObjectCacheResourceInjected.CacheLoader;
import fi.luomus.triplestore.dao.SearchParams;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.models.TaxonGroupIucnEditors;
import fi.luomus.triplestore.taxonomy.models.TaxonGroupIucnEvaluationData;
import fi.luomus.triplestore.taxonomy.models.TaxonGroupIucnEvaluationData.SpeciesInfo;

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

	private final Config config;
	private final TriplestoreDAO triplestoreDAO;

	public IucnDAOImple(Config config, TriplestoreDAO triplestoreDAO) {
		this.config = config;
		this.triplestoreDAO = triplestoreDAO;
	}

	private static final SingleObjectCacheResourceInjected<Map<String, TaxonGroupIucnEditors>, TriplestoreDAO> 
	cachedGroupEditors = 
	new SingleObjectCacheResourceInjected<>(
			new CacheLoader<Map<String, TaxonGroupIucnEditors>, TriplestoreDAO>() {
				@Override
				public Map<String, TaxonGroupIucnEditors> load(TriplestoreDAO dao) {
					try {
						Map<String,TaxonGroupIucnEditors> map = new HashMap<>();
						for (Model m : dao.getSearchDAO().search("rdf:type", "MKV.taxonGroupIucnEditors")) {
							String groupQname = m.getStatements("MKV.taxonGroup").get(0).getObjectResource().getQname();
							TaxonGroupIucnEditors groupEditors = new TaxonGroupIucnEditors(new Qname(m.getSubject().getQname()), new Qname(groupQname));
							for (Statement editor : m.getStatements("MKV.iucnEditor")) {
								groupEditors.addEditor(new Qname(editor.getObjectResource().getQname()));
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
	public Map<String, TaxonGroupIucnEditors> getGroupEditors() throws Exception {
		return cachedGroupEditors.get(triplestoreDAO);
	}

	@Override
	public void clearEditorCache() {
		cachedGroupEditors.invalidate();
	}

	private static final SingleObjectCacheResourceInjected<List<Integer>, TriplestoreDAO> 
	evaluationYearsCache = 
	new SingleObjectCacheResourceInjected<>(
			new CacheLoader<List<Integer>, TriplestoreDAO>() {
				@Override
				public List<Integer> load(TriplestoreDAO dao) {
					List<Integer> evaluationYears = new ArrayList<>();
					try {
						for (Model m : dao.getSearchDAO().search("rdf:type", "MKV.iucnRedListEvaluationYear")) {
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
		return evaluationYearsCache.get(triplestoreDAO);
	}

	private static final Map<String, TaxonGroupIucnEvaluationData> taxonGroupData = new HashMap<>();
	private static final Object LOCK = new Object();

	@Override
	public TaxonGroupIucnEvaluationData getTaxonGroupData(String groupQname) throws Exception {
		if (taxonGroupData.containsKey(groupQname)) return taxonGroupData.get(groupQname);
		synchronized (LOCK) {
			if (taxonGroupData.containsKey(groupQname)) return taxonGroupData.get(groupQname);
			TaxonGroupIucnEvaluationData groupData = loadGroupData(groupQname);
			taxonGroupData.put(groupQname, groupData);
			return groupData;
		}
	}

	private TaxonGroupIucnEvaluationData loadGroupData(String groupQname) throws Exception {
		List<SpeciesInfo> speciesOfGroup = loadSpeciesOfGroup(groupQname);
		TaxonGroupIucnEvaluationData data = new TaxonGroupIucnEvaluationData(new Qname(groupQname), speciesOfGroup);
		addEvaluations(speciesOfGroup, data);
		return data;
	}

	private void addEvaluations(List<SpeciesInfo> speciesOfGroup, TaxonGroupIucnEvaluationData data) throws Exception {
		for (SpeciesInfo species : speciesOfGroup) {
			addEvaluations(data, species.getQname());
		}
	}

	private void addEvaluations(TaxonGroupIucnEvaluationData data, String speciesQname) throws Exception {
		Collection<Model> evaluations = getEvaluations(speciesQname);
		for (Model evaluation : evaluations) {
			int year = getEvaluationYear(evaluation);
			data.getYear(year).setEvaluation(speciesQname, evaluation);
		}
	}

	private int getEvaluationYear(Model evaluation) {
		return Integer.valueOf(evaluation.getStatements("MKV.evaluationYear").get(0).getObjectLiteral().getContent());
	}

	private Collection<Model> getEvaluations(String speciesQname) throws Exception {
		return triplestoreDAO.getSearchDAO().search(
				new SearchParams(Integer.MAX_VALUE, 0)
				.type("MKV.iucnRedListEvaluation")
				.predicate("MKV.evaluatedTaxon")
				.objectresource(speciesQname));
	}

	private List<SpeciesInfo> loadSpeciesOfGroup(String groupQname) throws Exception {
		Set<String> rootTaxonsOfGroup = getRootTaxonsOfGroup(groupQname);
		List<SpeciesInfo> speciesOfGroup = new ArrayList<>();
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
			if (!"MR.1".equals(checklist)) continue;
			rootTaxonsOfGroup.add(m.getSubject().getQname());
		}
		return rootTaxonsOfGroup;
	}

	private void addSpeciesOfTaxon(List<SpeciesInfo> speciesOfGroup, HttpClientService client, String rootTaxonQname) throws Exception {
		System.out.println("Loading finnish species for " + rootTaxonQname);
		URI uri = new URI(config.get("TaxonomyAPIURL")+"/" + rootTaxonQname + "/finnish/species?selectedFields=qname,scientificName,vernacularNamesWithLangCodes");
		JSONObject response = client.contentAsJson(new HttpGet(uri));
		for (JSONObject species : response.getArray("children").iterateAsObject()) {
			SpeciesInfo speciesInfo = getSpeciesInfo(species);
			speciesOfGroup.add(speciesInfo);
		}
	}

	private SpeciesInfo getSpeciesInfo(JSONObject species) {
		String qname = species.getObject("qname").getString("qname");
		String scientificName = species.hasKey("scientificName") ? species.getString("scientificName") : null;
		String vernacularNameFi = null;
		for (JSONObject name : species.getArray("vernacularNamesWithLangCodes").iterateAsObject()) {
			if (name.getString("locale").equals("fi")) {
				vernacularNameFi = name.getString("name");
				break;
			}
		}
		SpeciesInfo speciesInfo = new SpeciesInfo(qname, scientificName, vernacularNameFi);
		return speciesInfo;
	}

}
