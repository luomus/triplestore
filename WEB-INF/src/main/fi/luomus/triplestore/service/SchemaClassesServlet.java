package fi.luomus.triplestore.service;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.json.JSONObject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.dao.SearchParams;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.dao.TriplestoreDAO.ResultType;
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

	protected ResponseData processGetWithAccess(HttpServletRequest req, HttpServletResponse res) throws Exception, IOException {
		TriplestoreDAO dao = getTriplestoreDAO();
		Set<Qname> classQnames = getQnamesOfType(dao, "rdfs:Class");
		Collection<Model> models = dao.getSearchDAO().get(classQnames, ResultType.NORMAL);
		return jsonResponse(new JSONObject(), res);
	}

	protected Set<Qname> getQnamesOfType(TriplestoreDAO dao, String type) throws Exception {
		Collection<Model> properties = dao.getSearchDAO().search(new SearchParams().type(type));
		Set<Qname> propertyQnames = new HashSet<>();
		for (Model m : properties) {
			propertyQnames.add(new Qname(m.getSubject().getQname()));
		}
		return propertyQnames;
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
