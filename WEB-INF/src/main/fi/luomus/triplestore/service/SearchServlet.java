package fi.luomus.triplestore.service;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.dao.TriplestoreDAO;

import java.util.Collection;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.json.XML;

@WebServlet(urlPatterns = {"/search/*"})
public class SearchServlet extends ApiServlet {

	private static final long serialVersionUID = -6581336459660016851L;

	public static final int DEFAULT_MAX_RESULT_COUNT = 1000;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		String[] subjects = req.getParameterValues("subject");
		String[] predicates = req.getParameterValues("predicate");
		String[] objects = req.getParameterValues("object");
		String[] objectresources = req.getParameterValues("objectresource");
		String[] objectliterals = req.getParameterValues("objectliteral");
		String type = req.getParameter("type");
		
		TriplestoreDAO dao = getTriplestoreDAO();
		Format format = getFormat(req);
		int limit = getLimit(req);
		int offset = getOffset(req);

		if (noneGiven(subjects) && noneGiven(predicates) && noneGiven(objects) && noneGiven(objectresources) && noneGiven(objectliterals) && notGiven(type)) {
			return redirectTo500(res);
		}

		String response = search(subjects, predicates, objects, objectresources, objectliterals, type, limit, offset, format, dao);

		if (jsonRequest(format)) {
			return jsonResponse(response, res);
		} else {
			return rdfResponse(response, res);
		}
	}

	public static String search(String[] subjects, String[] predicates, String[] objects, String[] objectresources, String[] objectliterals, String type, int limit, int offset, Format format, TriplestoreDAO dao) throws Exception {
		Collection<Model> models = dao.getSearchDAO().search(subjects, predicates, objects, objectresources, objectliterals, type, limit, offset);

		String rdf = generateRdf(models, format);

		if (jsonRequest(format)) {
			JSONObject jsonObject = XML.toJSONObject(rdf);
			String json = jsonObject.toString();
			return json;
		} else {
			return rdf;
		}
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

	private boolean noneGiven(String[] values) {
		if (values == null) return true;
		if (values.length == 0) return true;
		for (String v : values) {
			if (!notGiven(v)) return false;
		}
		return true;
	}

	private boolean notGiven(String value) {
		return !given(value);
	}

	private static boolean given(String value) {
		return value != null && value.length() > 0;
	}

}
