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

@WebServlet(urlPatterns = {"/schema/property/*"})
public class SchemaPropertiesServlet extends ApiServlet {

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

	private ResponseData processGetWithAccess(HttpServletRequest req, HttpServletResponse res) throws Exception, IOException {
		TriplestoreDAO dao = getTriplestoreDAO();
		Set<Qname> propertyQnames = getQnamesOfType(dao, "rdf:Property");
		Collection<Model> models = dao.getSearchDAO().get(propertyQnames, ResultType.NORMAL);
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
