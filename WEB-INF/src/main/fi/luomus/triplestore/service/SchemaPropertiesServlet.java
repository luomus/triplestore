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

@WebServlet(urlPatterns = {"/schema/property/*"})
public class SchemaPropertiesServlet extends SchemaClassesServlet {

	private static final long serialVersionUID = 6301235108157969958L;

	@Override
	protected ResponseData processGetWithAccess(HttpServletRequest req, HttpServletResponse res) throws Exception, IOException {
		TriplestoreDAO dao = getTriplestoreDAO();
		Collection<Model> models = dao.getSearchDAO().search(new SearchParams(Integer.MAX_VALUE, 0).type("rdf:Property"));
		JSONArray response = parsePropertiesResponse(models);
		return jsonResponse(response, res);
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
	//	        "required": false, // minOccurs !== '0' (tyhjä required true)
	//	        "hasMany": false, //  maxOccurs on > 1
	//	        "sortOrder": -1, // sortorder-arvo tai -1 jos tyhjä
	//	        "isEmbeddable": false, // MZ.embeddable -arvo jos annettu, false jos tyhjä
	//	        "shortName": "invasiveCitizenActionsText"
	//	    },
	private JSONArray parsePropertiesResponse(Collection<Model> models) {
		JSONArray response = new JSONArray();
		for (Model model : models) {
			parsePropertiesResponse(response, model);
		}
		return response;
	}

	private void parsePropertiesResponse(JSONArray response, Model model) {
		JSONObject propertyJson = new JSONObject();
		propertyJson.setString("property", model.getSubject().getQname());
		labels(propertyJson, model);
		propertyJson.setArray("domain", domains(model));
		propertyJson.setArray("range", ranges(model));
		String minOccurs = occurs(model, "xsd:minOccurs");
		String maxOccurs = occurs(model, "xsd:maxOccurs");
		propertyJson.setString("minOccurs", minOccurs);
		propertyJson.setString("maxOccurs", maxOccurs);
		propertyJson.setBoolean("required", !minOccurs.equals("0"));
		propertyJson.setBoolean("hasMany", !maxOccurs.equals("1"));
		propertyJson.setInteger("sortOrder", sortOrder(model));
		propertyJson.setBoolean("isEmbeddable", embeddable(model));
		propertyJson.setBoolean("multiLanguage", multiLanguage(model));
		shortName(propertyJson, model);
		response.appendObject(propertyJson);
	}

	private boolean multiLanguage(Model model) {
		return getBoolean(model, "MZ.multiLanguage");
	}

	private boolean embeddable(Model model) {
		return getBoolean(model, "MZ.embeddable");
	}

	private boolean getBoolean(Model model, String field) {
		if (!model.hasStatements(field)) return false;
		Statement s = model.getStatements(field).get(0);
		if (s.isLiteralStatement()) {
			return "true".equals(s.getObjectLiteral().getContent());
		}
		return false;
	}

	private int sortOrder(Model model) {
		if (!model.hasStatements("sortOrder")) return -1;
		Statement s = model.getStatements("sortOrder").get(0);
		if (s.isLiteralStatement()) {
			try {
				return Integer.valueOf(s.getObjectLiteral().getContent());
			} catch (NumberFormatException e) {
				return -1;
			}
		}
		return -1;
	}

	private String occurs(Model model, String predicate) {
		if (!model.hasStatements(predicate)) return "1";
		Statement statement = model.getStatements(predicate).get(0);
		String occurs = "1";
		if (statement.isLiteralStatement()) {
			occurs = statement.getObjectLiteral().getContent(); 
		}
		if (!given(occurs)) return "1";
		return occurs;
	}

	private JSONArray ranges(Model model) {

		return qnames(model, "rdfs:range");
	}

	private JSONArray qnames(Model model, String predicate) {
		JSONArray json = new JSONArray();
		for (Statement statement : model.getStatements(predicate)) {
			if (statement.isResourceStatement()) {
				json.appendString(statement.getObjectResource().getQname());
			}
		}
		return json;
	}

	private JSONArray domains(Model model) {
		return qnames(model, "rdfs:domain");
	}

}
