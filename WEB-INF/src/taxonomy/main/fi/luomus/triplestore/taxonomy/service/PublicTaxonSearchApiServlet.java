package fi.luomus.triplestore.taxonomy.service;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.XML;

import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.json.JSONObject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.taxonomy.TaxonSearch;
import fi.luomus.commons.utils.Utils;
import fi.luomus.commons.xml.Document;
import fi.luomus.commons.xml.Document.Node;
import fi.luomus.commons.xml.XMLWriter;

@WebServlet(urlPatterns = {"/taxon-search/*", "/taxonomy-editor/api/taxon-search/*"})
public class PublicTaxonSearchApiServlet extends TaxonomyEditorBaseServlet {

	private static final String NULL = "null";
	private static final String LIMIT = "limit";
	private static final String ONLY_FINNISH = "onlyFinnish";
	private static final String ONLY_SPECIES = "onlySpecies";
	private static final String ONLY_EXACT = "onlyExact";
	private static final String TRUE = "true";
	private static final String ERROR = "error";
	private static final String CALLBACK = "callback";
	private static final String EXACT_MATCHES = "exactMatches";
	private static final String PARTIAL_MATCHES = "partialMatches";
	private static final String LIKELY_MATCHES = "likelyMatches";
	private static final String EXACT_MATCH = "exactMatch";
	private static final String CURSIVE_NAME = "cursiveName";
	private static final String SPECIES = "species";
	private static final String TAXON_RANK_ID = "taxonRankId";
	private static final String SCIENTIFIC_NAME = "scientificName";
	private static final String SCIENTIFIC_NAME_AUTHORSHIP = "scientificNameAuthorship";
	private static final String EN = "en";
	private static final String SV = "sv";
	private static final String FI = "fi";
	private static final String ID = "id";
	private static final String NAME = "name";
	private static final String TAXON_RANK = "taxonRank";
	private static final String CHECKLIST = "checklist";
	private static final String REQUIRED_INFORMAL_TAXON_GROUP = "requiredInformalTaxonGroup";
	private static final String MATCHING_NAME = "matchingName";
	private static final String INFORMAL_GROUPS = "informalGroups";
	private static final int DEFAULT_LIMIT = Integer.MAX_VALUE;
	private static final long serialVersionUID = -1055689074656680611L;

	@Override
	protected boolean authorized(HttpServletRequest req) {
		return true;
	}

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		String searchword = req.getParameter("q");
		if (!given(searchword)) {
			searchword = Utils.urlDecode(getQname(req)); // the path needs to encoded and decoded.. but it fails for some cases. It is better to use the "q"-parameter.
		}
		int limit = getLimit(req);
		Qname checklist = parseChecklist(req);
		Set<Qname> requiredInformalGroups = parseRequiredInformalGroups(req);
		boolean onlyExact = TRUE.equals(req.getParameter(ONLY_EXACT));
		Boolean onlySpecies = null;
		boolean onlyFinnish = TRUE.equals(req.getParameter(ONLY_FINNISH));
		if (TRUE.equals(req.getParameter(ONLY_SPECIES))) onlySpecies = true;
		TaxonSearch taxonSearch = new TaxonSearch(searchword, limit, checklist).setOnlyFinnish(onlyFinnish).setSpecies(onlySpecies);
		for (Qname q : requiredInformalGroups) {
			taxonSearch.addInformalTaxonGroup(q);
		}
		if (onlyExact) taxonSearch.onlyExact();
		
		int version = getVersion(req);

		Format format = getFormat(req);

		Document response = getTaxonomyDAO().search(taxonSearch).getResultsAsDocument();
		if (response.getRootNode().hasAttribute(ERROR)) {
			if (response.getRootNode().getAttribute(ERROR).startsWith("Search word")) {
				res.setStatus(400);
			} else {
				res.setStatus(500);
			}
		}
		if (format == Format.JSONP) {
			String callback = req.getParameter(CALLBACK);
			if (callback == null || callback.length() < 1) throw new IllegalArgumentException("Callback parameter must be given for jsonp response. Use 'callback'.");
			return jsonpResponse(toJsonp(response, callback, version), res);
		}
		if (jsonRequest(format)) {
			if (version == 2) {
				return jsonResponse(toJsonV2(response).toString(), res);
			} else {
				String xml = new XMLWriter(response).generateXML();
				org.json.JSONObject jsonObject = XML.toJSONObject(xml);
				String json = jsonObject.toString();
				return jsonResponse(json, res);
			}
		} else {
			return xmlResponse(response, res);
		}
	}

	private JSONObject toJsonV2(Document response) {
		JSONObject json = new JSONObject();
		Node root = response.getRootNode();
		addJsonV2Matches(json, root, EXACT_MATCH, EXACT_MATCHES);
		addJsonV2Matches(json, root, LIKELY_MATCHES, LIKELY_MATCHES);
		addJsonV2Matches(json, root, PARTIAL_MATCHES, PARTIAL_MATCHES);
		return json;
	}

	private void addJsonV2Matches(JSONObject json, Node root, String nodeName, String fieldName) {
		if (root.hasChildNodes(nodeName)) {
			for (Node match : root.getNode(nodeName).getChildNodes()) {
				json.getArray(fieldName).appendObject(toJsonV2(match));
			}
		}
	}

	private JSONObject toJsonV2(Node match) {
		JSONObject json = new JSONObject();
		json.setString(ID, match.getName());
		json.setString(MATCHING_NAME, match.getAttribute(MATCHING_NAME));
		if (match.hasAttribute(SCIENTIFIC_NAME)) {
			json.setString(SCIENTIFIC_NAME, match.getAttribute(SCIENTIFIC_NAME));			
		}
		if (match.hasAttribute(SCIENTIFIC_NAME_AUTHORSHIP)) {
			json.setString(SCIENTIFIC_NAME_AUTHORSHIP, match.getAttribute(SCIENTIFIC_NAME_AUTHORSHIP));
		}
		Qname taxonRank = getTaxonRank(match);
		if (given(taxonRank)) {
			json.setString(TAXON_RANK_ID, taxonRank.toString());	
		}
		json.setBoolean(SPECIES, Taxon.isSpecies(taxonRank));
		json.setBoolean(CURSIVE_NAME, Taxon.shouldCursive(taxonRank));
		if (match.hasChildNodes(INFORMAL_GROUPS)) {
			for (Node group : match.getNode(INFORMAL_GROUPS).getChildNodes()) {
				json.getArray(INFORMAL_GROUPS).appendObject(toJSONV2InformalGroup(group));
			}
		}
		return json;
	}

	private JSONObject toJSONV2InformalGroup(Node group) {
		JSONObject json = new JSONObject();
		json.setString(ID, group.getName());
		addJSONV2GroupName(group, json, FI);
		addJSONV2GroupName(group, json, SV);
		addJSONV2GroupName(group, json, EN);
		return json;
	}

	private void addJSONV2GroupName(Node group, JSONObject json, String locale) {
		if (group.hasAttribute(locale)) {
			json.getObject(NAME).setString(locale, group.getAttribute(locale));
		}
	}

	private Qname getTaxonRank(Node match) {
		if (match.hasAttribute(TAXON_RANK)) return new Qname(match.getAttribute(TAXON_RANK));
		return null;
	}

	private int getVersion(HttpServletRequest req) {
		String version = req.getParameter("v");
		if (version == null) return 1;
		return Integer.valueOf(version);
	}

	private Set<Qname> parseRequiredInformalGroups(HttpServletRequest req) {
		if (req.getParameter(REQUIRED_INFORMAL_TAXON_GROUP) == null) return Collections.emptySet();
		String[] groups = req.getParameterValues(REQUIRED_INFORMAL_TAXON_GROUP);
		Set<Qname> set = new HashSet<>();
		for (String group : groups) {
			for (String groupPart : group.split(Pattern.quote(","))) {
				if (given(groupPart)) set.add(new Qname(groupPart));
			}
		}
		return set;
	}

	public static Qname parseChecklist(HttpServletRequest req) {
		String checklistParameter = req.getParameter(CHECKLIST);
		if (!given(checklistParameter)) {
			return TaxonSearch.MASTER_CHECKLIST;
		} 
		if (checklistParameter.equals(NULL)) {
			return null;
		}
		return new Qname(checklistParameter);
	}

	private int getLimit(HttpServletRequest req) {
		String limit = req.getParameter(LIMIT);
		if (limit == null) return DEFAULT_LIMIT;
		try {
			return Integer.valueOf(limit);
		} catch (Exception e) {
			return DEFAULT_LIMIT;
		}
	}

	private ResponseData jsonpResponse(String response, HttpServletResponse res) throws Exception {
		res.setContentType("application/javascript; charset=utf-8");
		res.setHeader("Access-Control-Allow-Origin", "*");
		PrintWriter out = res.getWriter();
		out.write(response);
		out.flush();
		return new ResponseData().setOutputAlreadyPrinted();
	}

	protected String toJsonp(Document results, String callback, int version) {
		if (version == 1) {
			return toJsonp(results, callback);
		}
		if (version == 2) {
			return toJsonpv2(results, callback);
		}
		throw new UnsupportedOperationException("Version " + version);
	}
	
	private String toJsonpv2(Document results, String callback) {
		JSONObject res = new JSONObject();
		Set<String> alreadyAdded = new HashSet<>();
		for (Node matchType : results.getRootNode()) {
			for (Node match : matchType) {
				String id = match.getName();
				String name = match.getAttribute(MATCHING_NAME).replace("'", "\\'");
				String test = id+name;
				if (alreadyAdded.contains(test)) continue;
				String group = match.hasChildNodes(INFORMAL_GROUPS) && match.getNode(INFORMAL_GROUPS).hasChildNodes() ? 
						match.getNode(INFORMAL_GROUPS).getChildNodes().get(0).getAttribute(EN) : null;
				String label = group == null ? name : name + " [" + group + "]";
				JSONObject resultEntry = new JSONObject();
				resultEntry.setString("value", id);
				resultEntry.setString("label", label);
				res.getArray("result").appendObject(resultEntry);
				alreadyAdded.add(test);
			}
		}
		return callback + "(" + res.toString() + ");";
	}

	protected String toJsonp(Document results, String callback) {
		StringBuilder out = new StringBuilder();
		out.append(callback + "({ result: [");

		Set<String> matches = new LinkedHashSet<String>();
		for (Node matchType : results.getRootNode()) {
			for (Node match : matchType) {
				String name = match.getAttribute(MATCHING_NAME).replace("'", "\\'");
				matches.add(name);
			}
		}
		Iterator<String> i = matches.iterator();
		while (i.hasNext()) {
			out.append("'"+i.next()+"'");
			if (i.hasNext()) out.append(", ");
		}
		out.append("] });");
		return out.toString();
	}

}
