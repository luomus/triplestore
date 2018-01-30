package fi.luomus.triplestore.service;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.json.JSONObject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.dao.TriplestoreDAO.ResultType;

@WebServlet(urlPatterns = {"/schema/alt/*"})
public class SchemaAltsServlet extends SchemaClassesServlet {

	private static final long serialVersionUID = -7921975069788844624L;

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

	@Override
	protected ResponseData processGetWithAccess(HttpServletRequest req, HttpServletResponse res) throws Exception, IOException {
		TriplestoreDAO dao = getTriplestoreDAO();
		Set<Qname> altQnames = getQnamesOfType(dao, "rdf:Alt");
		Collection<Model> models = dao.getSearchDAO().get(altQnames, ResultType.DEEP);
		return jsonResponse(new JSONObject(), res);
	}

}
