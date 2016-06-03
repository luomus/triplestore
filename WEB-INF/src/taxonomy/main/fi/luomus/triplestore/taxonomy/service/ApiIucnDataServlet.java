package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.http.HttpClientService;
import fi.luomus.commons.json.JSONObject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.dao.SearchParams;
import fi.luomus.triplestore.taxonomy.models.TaxonGroupIucnEvaluationData;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.HttpGet;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/iucn-data/*"})
public class ApiIucnDataServlet extends ApiBaseServlet {

	private static final long serialVersionUID = -3859060737786587345L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		String groupQname = getId(req);
		int year = Integer.valueOf(req.getParameter("year"));
		if (!given(groupQname)) return redirectTo404(res);
		TaxonGroupIucnEvaluationData data = getTaxonGroupData(groupQname);

		return new ResponseData().setViewName("iucn-stat").setData("data", data).setData("year", year);
	}

	private static final Map<String, TaxonGroupIucnEvaluationData> taxonGroupData = new HashMap<>();
	private static final Object LOCK = new Object();

	protected TaxonGroupIucnEvaluationData getTaxonGroupData(String groupQname) throws Exception {
		if (taxonGroupData.containsKey(groupQname)) return taxonGroupData.get(groupQname);
		synchronized (LOCK) {
			if (taxonGroupData.containsKey(groupQname)) return taxonGroupData.get(groupQname);
			TaxonGroupIucnEvaluationData groupData = loadGroupData(groupQname);
			taxonGroupData.put(groupQname, groupData);
			return groupData;
		}
	}

	private TaxonGroupIucnEvaluationData loadGroupData(String groupQname) throws Exception {
		Set<String> rootTaxonsOfGroup = getRootTaxonsOfGroup(groupQname);
		Set<String> speciesOfGroup = new LinkedHashSet<>();
		HttpClientService client = null;
		try {
			client = new HttpClientService();
			for (String rootTaxonQname : rootTaxonsOfGroup) {
				addSpeciesOfTaxon(speciesOfGroup, client, rootTaxonQname);
			}
		} finally {
			if (client !=  null) client.close();
		}
		return new TaxonGroupIucnEvaluationData(new Qname(groupQname), speciesOfGroup);
	}

	private Set<String> getRootTaxonsOfGroup(String groupQname) throws Exception {
		SearchParams searchParams = new SearchParams(Integer.MAX_VALUE, 0).predicate("MX.isPartOfInformalTaxonGroup").objectresource(groupQname);
		Set<String> rootTaxonsOfGroup = new HashSet<>();
		for (Model m : getTriplestoreDAO().getSearchDAO().search(searchParams)) {
			if (!m.hasStatements("MX.nameAccordingTo")) continue;
			String checklist = m.getStatements("MX.nameAccordingTo").get(0).getObjectResource().getQname();
			if (!"MR.1".equals(checklist)) continue;
			rootTaxonsOfGroup.add(m.getSubject().getQname());
		}
		return rootTaxonsOfGroup;
	}

	private void addSpeciesOfTaxon(Set<String> speciesOfGroup, HttpClientService client, String rootTaxonQname) throws Exception {
		System.out.println("Loading finnish species for " + rootTaxonQname);
		URI uri = new URI(getConfig().get("TaxonomyAPIURL")+"/" + rootTaxonQname + "/finnish/species?selectedFields=qname");
		JSONObject response = client.contentAsJson(new HttpGet(uri));
		for (JSONObject species : response.getArray("children").iterateAsObject()) {
			String qname = species.getObject("qname").getString("qname");
			speciesOfGroup.add(qname);
		}
	}

}
