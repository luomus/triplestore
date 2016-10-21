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
import fi.luomus.commons.taxonomy.TaxonomyDAO;
import fi.luomus.commons.taxonomy.TaxonomyDAO.TaxonSearch;
import fi.luomus.commons.utils.Cached;
import fi.luomus.commons.utils.Cached.CacheLoader;
import fi.luomus.commons.utils.Utils;
import fi.luomus.commons.xml.Document;
import fi.luomus.commons.xml.Document.Node;
import fi.luomus.commons.xml.XMLWriter;

@WebServlet(urlPatterns = {"/taxon-search/*", "/taxonomy-editor/api/taxon-search/*"})
public class PublicTaxonSearchApiServlet extends TaxonomyEditorBaseServlet {

	private static final int DEFAULT_LIMIT = Integer.MAX_VALUE;

	private static final long serialVersionUID = -1055689074656680611L;

	private static final int ONE_HOUR_IN_SECONDS = 60*60*1;
	private static final long MAX_TAXON_SEARCH_CACHE_ITEM_COUNT = 20000;
	private static final Qname MASTER_CHECKLIST = new Qname("MR.1");

	@Override
	protected boolean authorized(HttpServletRequest req) {
		return true;
	}

	private class SearchWrapper {
		private final TaxonomyDAO dao;
		private final String searchword;
		private final Qname checklist;
		private final int limit;
		private final Set<Qname> requiredInformalGroups;
		private final String toString;
		public SearchWrapper(TaxonomyDAO dao, String searchword, Qname checklist, int limit, Set<Qname> requiredInformalGroups) {
			this.dao = dao;
			this.searchword = searchword;
			this.checklist = checklist;
			this.limit = limit;
			this.requiredInformalGroups = requiredInformalGroups;
			this.toString = this.searchword + " (" + checklist + ") + limit:" + limit + " required groups: " + requiredInformalGroups;
		}
		@Override
		public int hashCode() {
			return this.toString().hashCode();
		}
		@Override
		public boolean equals(Object o) {
			return this.toString().equals(o.toString());
		}
		@Override
		public String toString() {
			return toString;
		}
		public TaxonSearch toSearch() {
			TaxonSearch taxonSearch = new TaxonSearch(searchword, limit, checklist);
			for (Qname group : requiredInformalGroups) {
				taxonSearch.addInformalTaxonGroup(group);
			}
			return taxonSearch;
		}
	}

	private static CacheLoader<SearchWrapper, Document> loader = new CacheLoader<SearchWrapper, Document>() {

		@Override
		public Document load(SearchWrapper wrapper) {
			try {
				return wrapper.dao.search(wrapper.toSearch());
			} catch (Exception e) {
				throw new RuntimeException(wrapper.toString(), e);
			}
		}
	};

	private static Cached<SearchWrapper, Document> cachedSearches = new Cached<SearchWrapper, Document>(loader, ONE_HOUR_IN_SECONDS, MAX_TAXON_SEARCH_CACHE_ITEM_COUNT);

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		String searchword = req.getParameter("q");
		if (!given(searchword)) {
			searchword = Utils.urlDecode(getQname(req)); // Yes, the path needs to encoded and decoded.. but it fails for some cases. It is not reccomended to use parameter
		}
		int limit = getLimit(req);
		Qname checklist = parseChecklist(req);
		Set<Qname> requiredInformalGroups = parseRequiredInformalGroups(req);
		int version = getVersion(req);

		Format format = getFormat(req);

		Document response = cachedSearches.get(new SearchWrapper(getTaxonomyDAO(), searchword, checklist, limit, requiredInformalGroups));
		if (response.getRootNode().hasAttribute("error")) {
			if (response.getRootNode().getAttribute("error").startsWith("Search word")) {
				res.setStatus(400);
			} else {
				res.setStatus(500);
			}
		}
		if (format == Format.JSONP) {
			String callback = req.getParameter("callback");
			if (callback == null || callback.length() < 1) throw new IllegalArgumentException("Callback parameter must be given for jsonp response. Use 'callback'.");
			return jsonpResponse(toJsonp(response, callback), res);
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
		addJsonV2Matches(json, root, "exactMatch", "exactMatches");
		addJsonV2Matches(json, root, "likelyMatches", "likelyMatches");
		addJsonV2Matches(json, root, "partialMatches", "partialMatches");
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
		json.setString("id", match.getName());
		json.setString("matchingName", match.getAttribute("matchingName"));
		json.setString("scientificName", match.getAttribute("scientificName"));
		if (match.hasAttribute("scientificNameAuthorship")) {
			json.setString("scientificNameAuthorship", match.getAttribute("scientificNameAuthorship"));
		}
		Qname taxonRank = getTaxonRank(match);
		if (given(taxonRank)) {
			json.setString("taxonRankId", taxonRank.toString());	
		}
		json.setBoolean("isSpecies", Taxon.isSpecies(taxonRank));
		json.setBoolean("isCursiveName", Taxon.shouldCusive(taxonRank));
		if (match.hasChildNodes("informalGroups")) {
			for (Node group : match.getNode("informalGroups").getChildNodes()) {
				json.getArray("informalGroups").appendObject(toJSONV2InformalGroup(group));
			}
		}
		return json;
	}

	private JSONObject toJSONV2InformalGroup(Node group) {
		JSONObject json = new JSONObject();
		json.setString("id", group.getName());
		addJSONV2GroupName(group, json, "fi");
		addJSONV2GroupName(group, json, "sv");
		addJSONV2GroupName(group, json, "en");
		return json;
	}

	private void addJSONV2GroupName(Node group, JSONObject json, String locale) {
		if (group.hasAttribute(locale)) {
			json.getObject("name").setString(locale, group.getAttribute(locale));
		}
	}

	private Qname getTaxonRank(Node match) {
		if (match.hasAttribute("taxonRank")) return new Qname(match.getAttribute("taxonRank"));
		return null;
	}

	private int getVersion(HttpServletRequest req) {
		String version = req.getParameter("v");
		if (version == null) return 1;
		return Integer.valueOf(version);
	}

	private Set<Qname> parseRequiredInformalGroups(HttpServletRequest req) {
		if (req.getParameter("requiredInformalTaxonGroup") == null) return Collections.emptySet();
		String[] groups = req.getParameterValues("requiredInformalTaxonGroup");
		Set<Qname> set = new HashSet<>();
		for (String group : groups) {
			for (String groupPart : group.split(Pattern.quote(","))) {
				if (given(groupPart)) set.add(new Qname(groupPart));
			}
		}
		return set;
	}

	private Qname parseChecklist(HttpServletRequest req) {
		String checklistParameter = req.getParameter("checklist");
		if (!given(checklistParameter)) {
			return MASTER_CHECKLIST;
		} 
		if (checklistParameter.equals("null")) {
			return null;
		}
		return new Qname(checklistParameter);
	}

	private int getLimit(HttpServletRequest req) {
		String limit = req.getParameter("limit");
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

	protected String toJsonp(Document results, String callback) {
		StringBuilder out = new StringBuilder();
		out.append(callback + "({ result: [");

		Set<String> matches = new LinkedHashSet<String>();
		for (Node matchType : results.getRootNode()) {
			for (Node match : matchType) {
				String name = match.getAttribute("matchingName").replace("'", "\\'");
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
