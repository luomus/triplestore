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

@WebServlet(urlPatterns = {"/schema/alt/*"})
public class SchemaAltsServlet extends ApiServlet {

	private static final long serialVersionUID = -4739626887383257765L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		Access access = getConnectionLimiter().delayAccessIfNecessary(req.getRemoteUser());
		try {
			return processGetWithAccess(req, res);
		} finally {
			access.release();
		}
	}

//	{
//	    "HRA.sentTypes": [
//	        {
//	            "id": "HRA.sentTypePriority",
//	            "value": {
//	                "en": "Priority mail",
//	                "fi": "",
//	                "sv": ""
//	            }
//	        },
	        
	private ResponseData processGetWithAccess(HttpServletRequest req, HttpServletResponse res) throws Exception, IOException {
		TriplestoreDAO dao = getTriplestoreDAO();
		Set<Qname> altQnames = getQnamesOfType(dao, "rdf:Alt");
		Collection<Model> models = dao.getSearchDAO().get(altQnames, ResultType.DEEP);
		return jsonResponse(new JSONObject(), res);
	}

	private Set<Qname> getQnamesOfType(TriplestoreDAO dao, String type) throws Exception {
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
