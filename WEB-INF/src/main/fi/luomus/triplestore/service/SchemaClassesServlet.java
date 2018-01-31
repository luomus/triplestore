package fi.luomus.triplestore.service;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.json.JSONArray;
import fi.luomus.commons.json.JSONObject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.dao.SearchParams;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.utils.ConnectionLimiter.Access;
import fi.luomus.triplestore.utils.StringUtils;

@WebServlet(urlPatterns = {"/schema/class/*"})
public class SchemaClassesServlet extends ApiServlet {

	private static final long serialVersionUID = 4677047722583381696L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		Access access = getConnectionLimiter().delayAccessIfNecessary(req.getRemoteUser());
		try {
			return processGetWithAccess(req, res);
		} finally {
			access.release();
		}
	}

	protected ResponseData processGetWithAccess(HttpServletRequest req, HttpServletResponse res) throws Exception, IOException {
		TriplestoreDAO dao = getTriplestoreDAO();
		Collection<Model> models = dao.getSearchDAO().search(new SearchParams().type("rdfs:Class"));
		JSONArray response = parseClassResponse(models);
		return jsonResponse(response, res);
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

	private String shortName(String qname) {
		return StringUtils.shortName(qname);
	}

	protected void labels(JSONObject json, Model model) {
		JSONObject labelJson = new JSONObject();
		for (Statement label : model.getStatements("rdfs:label")) {
			if (label.isLiteralStatement()) {
				labelJson.setString(label.getObjectLiteral().getLangcode(), label.getObjectLiteral().getContent());
			}
		}
		json.setObject("label", labelJson);
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
