package fi.luomus.triplestore.service;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.json.JSONArray;
import fi.luomus.commons.json.JSONObject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.utils.Cached;
import fi.luomus.commons.utils.Cached.CacheLoader;
import fi.luomus.triplestore.dao.SearchParams;
import fi.luomus.triplestore.dao.TriplestoreDAO;

@WebServlet(urlPatterns = {"/schema/class/*"})
public class SchemaClassesServlet extends ApiServlet {

	private static final long serialVersionUID = 4677047722583381696L;

	private CacheLoader<String, ResponseData> loader = new CacheLoader<String, ResponseData>() {
		
		@Override
		public ResponseData load(String type) {
			try {
				return generateResponse();
			} catch (Exception e) {
				getErrorReporter().report("Loading " + type, e);
				throw new RuntimeException(e);
			}
		}
	};
	
	private final Cached<String, ResponseData> cache = new Cached<>(loader, 15, TimeUnit.MINUTES, 3);  
	
	protected String type() {
		return "class";
	}
	
	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return cache.get(type());
	}

	protected ResponseData generateResponse() throws Exception, IOException {
		TriplestoreDAO dao = getTriplestoreDAO();
		Collection<Model> models = dao.getSearchDAO().search(new SearchParams(Integer.MAX_VALUE, 0).type("rdfs:Class"));
		JSONArray response = parseClassResponse(models);
		return jsonResponse(response);
	}

	//	[
	//
	//	    {
	//	        "class": "GX.dataset",
	//	        "label": {
	//	            "en": "Dataset",
	//	            "fi": "",
	//	            "sv": ""
	//	        },
	//	        "shortName": "dataset"
	//	    },
	private JSONArray parseClassResponse(Collection<Model> models) {
		JSONArray response = new JSONArray();
		for (Model model : models) {
			parseClassResponse(response, model);
		}
		return response;
	}

	private void parseClassResponse(JSONArray response, Model model) {
		JSONObject classJson = new JSONObject();
		classJson.setString("class", model.getSubject().getQname());
		labels(classJson, model);
		shortName(classJson, model);
		response.appendObject(classJson);
	}

	protected void shortName(JSONObject json, Model model) {
		json.setString("shortName", shortName(model));
	}

	private String shortName(Model model) {
		return shortName(model.getSubject().getQname());
	}

	protected void labels(JSONObject json, Model model) {
		json.setObject("label", labels(model));
	}

	protected JSONObject labels(Model model) {
		JSONObject labelJson = new JSONObject();
		for (Statement label : model.getStatements("rdfs:label")) {
			if (!label.isForDefaultContext()) continue;
			if (label.isLiteralStatement()) {
				labelJson.setString(label.getObjectLiteral().getLangcode(), label.getObjectLiteral().getContent());
			}
		}
		return labelJson;
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

	public static String shortName(String qname) {
		if (qname == null) return null;
		if (!qname.contains(".")) return qname;
		String[] parts = qname.split(Pattern.quote("."));
		if (parts.length == 2) {
			return parts[1];
		}
		String shortName = "";
		for (int i = 1; i<parts.length; i++) {
			shortName += parts[i];
			if (i < parts.length-1) shortName += ".";
		}
		return shortName;
	}
	
}
