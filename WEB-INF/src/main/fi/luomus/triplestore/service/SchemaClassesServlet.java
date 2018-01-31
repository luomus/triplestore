package fi.luomus.triplestore.service;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.json.JSONArray;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.dao.SearchParams;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.utils.ConnectionLimiter.Access;

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
		// TODO Auto-generated method stub
		return null;
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
