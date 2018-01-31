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

@WebServlet(urlPatterns = {"/schema/alt/*"})
public class SchemaAltsServlet extends SchemaClassesServlet {

	private static final long serialVersionUID = -7921975069788844624L;


	@Override
	protected ResponseData processGetWithAccess(HttpServletRequest req, HttpServletResponse res) throws Exception, IOException {
		TriplestoreDAO dao = getTriplestoreDAO();
		Set<Qname> altQnames = getQnamesOfType(dao, "rdf:Alt");
		Collection<Model> models = dao.getSearchDAO().get(altQnames, ResultType.DEEP);
		JSONObject response = parseAltsResponse(models); 
		return jsonResponse(response, res);
	}

	private Set<Qname> getQnamesOfType(TriplestoreDAO dao, String type) throws Exception {
		Collection<Model> models = dao.getSearchDAO().search(new SearchParams().type(type));
		Set<Qname> propertyQnames = new HashSet<>();
		for (Model m : models) {
			propertyQnames.add(new Qname(m.getSubject().getQname()));
		}
		return propertyQnames;
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
	private JSONObject parseAltsResponse(Collection<Model> models) {
		// TODO Auto-generated method stub
		return null;
	}

}
