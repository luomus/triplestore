package fi.luomus.triplestore.service;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.json.XML;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.xml.Document;
import fi.luomus.commons.xml.XMLWriter;
import fi.luomus.triplestore.dao.SearchParams;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.utils.AccessLimiter.Access;

@WebServlet(urlPatterns = {"/search/*"})
public class SearchServlet extends ApiServlet {

	private static final long serialVersionUID = -6581336459660016851L;

	public static final int DEFAULT_MAX_RESULT_COUNT = 1000;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		Set<String> subjects = getMultiParam("subject", req);
		Set<String> predicates = getMultiParam("predicate", req);
		Set<String> objects = getMultiParam("object", req);
		Set<String> objectresources = getMultiParam("objectresource", req);
		Set<String> objectliterals = getMultiParam("objectliteral", req);
		String type = req.getParameter("type");

		Format format = getFormat(req);
		int limit = getLimit(req);
		int offset = getOffset(req);

		if (noneGiven(subjects) && noneGiven(predicates) && noneGiven(objects) && noneGiven(objectresources) && noneGiven(objectliterals) && notGiven(type)) {
			return status500(res);
		}
		SearchParams searchParams = new SearchParams(limit, offset)
				.subjects(subjects).predicates(predicates).objects(objects)
				.objectresources(objectresources).objectliterals(objectliterals)
				.type(type);

		Access access = getAccessLimiter().delayAccessIfNecessary(req.getRemoteUser());
		try {
			return processGetWithAccess(req, format, searchParams);
		} finally {
			access.release();
		}
	}

	private ResponseData processGetWithAccess(HttpServletRequest req, Format format, SearchParams searchParams) throws Exception, IOException {
		TriplestoreDAO dao = getTriplestoreDAO();

		String response = null;
		if (countQuery(req)) {
			response = count(searchParams, format, dao);
		} else {
			response = search(searchParams, format, dao);
		}

		if (jsonRequest(format)) {
			return jsonResponse(response);
		}
		return rdfResponse(response);
	}

	protected Set<String> getMultiParam(String fieldName, HttpServletRequest req) {
		if (req.getParameter(fieldName) == null) return null;
		Set<String> fields = new HashSet<>();
		for (String param : req.getParameterValues(fieldName)) {
			param = param.trim();
			if (!given(param)) continue;
			for (String paramPart : param.split(Pattern.quote(","))) {
				paramPart = paramPart.trim();
				if (given(paramPart)) {
					fields.add(paramPart);
				}
			}
		}
		if (fields.isEmpty()) return null;
		return fields;
	}

	private String count(SearchParams searchParams, Format format, TriplestoreDAO dao) throws Exception {
		int count = dao.getSearchDAO().count(searchParams);
		if (format == Format.JSON) {
			fi.luomus.commons.json.JSONObject response = new fi.luomus.commons.json.JSONObject();
			response.setInteger("count", count);
			return response.reveal().toString();
		} else if (format == Format.XML) {
			Document response = new Document("countResponse");
			response.getRootNode().addAttribute("count", count);
			return new XMLWriter(response).generateXML();
		}
		throw new UnsupportedOperationException("Count response can not be given in " + format + " format.");
	}

	private boolean countQuery(HttpServletRequest req) {
		return req.getRequestURI().contains("/count");
	}

	public static String search(SearchParams searchParams, Format format, TriplestoreDAO dao) throws Exception {
		Collection<Model> models = dao.getSearchDAO().search(searchParams);
		String rdf = generateRdf(models, format);
		if (jsonRequest(format)) {
			JSONObject jsonObject = XML.toJSONObject(rdf);
			String json = jsonObject.toString();
			return json;
		}
		return rdf;
	}

	private int getLimit(HttpServletRequest req) {
		String limit = req.getParameter("limit");
		if (notGiven(limit)) return DEFAULT_MAX_RESULT_COUNT;
		try {
			return Integer.valueOf(limit);
		} catch (Exception e) {
			return DEFAULT_MAX_RESULT_COUNT;
		}
	}

	private int getOffset(HttpServletRequest req) {
		String offset = req.getParameter("offset");
		if (notGiven(offset)) return 0;
		try {
			return Integer.valueOf(offset);
		} catch (Exception e) {
			return 0;
		}
	}

	private boolean noneGiven(Set<String> values) {
		if (values == null) return true;
		if (values.isEmpty()) return true;
		for (String v : values) {
			if (!notGiven(v)) return false;
		}
		return true;
	}

	private boolean notGiven(String value) {
		return !given(value);
	}

	@Override
	protected ResponseData processDelete(HttpServletRequest req, HttpServletResponse res) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	protected ResponseData processPut(HttpServletRequest req, HttpServletResponse res) throws Exception {
		throw new UnsupportedOperationException();
	}
	
}
