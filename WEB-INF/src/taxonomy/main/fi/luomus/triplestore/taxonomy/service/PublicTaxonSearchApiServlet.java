package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.taxonomy.TaxonomyDAO;
import fi.luomus.commons.utils.Cached;
import fi.luomus.commons.utils.Cached.CacheLoader;
import fi.luomus.commons.utils.Utils;
import fi.luomus.commons.xml.Document;
import fi.luomus.commons.xml.Document.Node;
import fi.luomus.commons.xml.XMLWriter;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.json.XML;

@WebServlet(urlPatterns = {"/taxon-search/*", "/taxonomy-editor/api/taxon-search/*"})
public class PublicTaxonSearchApiServlet extends TaxonomyEditorBaseServlet {

	private static final int DEFAULT_LIMIT = Integer.MAX_VALUE;

	private static final long serialVersionUID = -1055689074656680611L;

	private static final int ONE_HOUR_IN_SECONDS = 60*60*1;
	private static final long MAX_TAXON_SEARCH_CACHE_ITEM_COUNT = 20000;
	private static final String MASTER_CHECKLIST_QNAME = "MR.1";

	@Override
	protected boolean authorized(HttpServletRequest req) {
		return true;
	}

	private class SearchWrapper {
		private final TaxonomyDAO dao;
		private final String searchword;
		private final String checklist;
		private final int limit;
		private final String toString;
		public SearchWrapper(TaxonomyDAO dao, String searchword, String checklist, int limit) {
			this.dao = dao;
			this.searchword = searchword;
			this.checklist = checklist;
			this.limit = limit;
			this.toString = this.searchword + " (" + checklist + ") + limit:" + limit;
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
	}

	private static CacheLoader<SearchWrapper, Document> loader = new CacheLoader<SearchWrapper, Document>() {

		@Override
		public Document load(SearchWrapper wrapper) {
			try {
				return wrapper.dao.search(wrapper.searchword, wrapper.checklist, wrapper.limit);
			} catch (Exception e) {
				throw new RuntimeException(wrapper.toString(), e);
			}
		}
	};

	private static Cached<SearchWrapper, Document> cachedSearches = new Cached<SearchWrapper, Document>(loader, ONE_HOUR_IN_SECONDS, MAX_TAXON_SEARCH_CACHE_ITEM_COUNT);

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		String searchword = Utils.urlDecode(getQname(req));
		String checklist = req.getParameter("checklist");
		int limit = getLimit(req);
		if (!given(checklist)) checklist = MASTER_CHECKLIST_QNAME;
		if (checklist.equals("null")) checklist = null;

		Format format = getFormat(req);

		Document response = cachedSearches.get(new SearchWrapper(getTaxonomyDAO(), searchword, checklist, limit));
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
			String xml = new XMLWriter(response).generateXML();
			JSONObject jsonObject = XML.toJSONObject(xml);
			String json = jsonObject.toString();
			return jsonResponse(json, res);
		} else {
			return xmlResponse(response, res);
		}
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
