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

@WebServlet(urlPatterns = {"/schema/property/*"})
public class SchemaPropertiesServlet extends SchemaClassesServlet {

	private static final long serialVersionUID = 6301235108157969958L;

	//	[
	//
	//	    {
	//	        "property": "MX.invasiveCitizenActionsText",
	//	        "label": {
	//	            "en": "What can I do?",
	//	            "fi": "Mitä minä voin tehdä?",
	//	            "sv": "Vad kan jag göra?"
	//	        },
	//	        "domain": [
	//	            "MX.taxon"
	//	        ],
	//	        "range": [
	//	            "xsd:string"
	//	        ],
	//	        "minOccurs": "0",
	//	        "maxOccurs": "1",
	//	        "required": false,
	//	        "hasMany": false,
	//	        "sortOrder": -1,
	//	        "isEmbeddable": false,
	//	        "shortName": "invasiveCitizenActionsText"
	//	    },

	//			"required": minOccurs !== '0' (tyhjä required true)
	//			"hasMany": maxOccurs on > 1
	//			"sortOrder": sortorder-arvo tai -1 jos tyhjä
	//			"isEmbeddable": MZ.embeddable -arvo jos annettu, false jos tyhjä

	@Override
	protected ResponseData processGetWithAccess(HttpServletRequest req, HttpServletResponse res) throws Exception, IOException {
		TriplestoreDAO dao = getTriplestoreDAO();
		Set<Qname> propertyQnames = getQnamesOfType(dao, "rdf:Property");
		Collection<Model> models = dao.getSearchDAO().get(propertyQnames, ResultType.NORMAL);
		return jsonResponse(new JSONObject(), res);
	}

}
