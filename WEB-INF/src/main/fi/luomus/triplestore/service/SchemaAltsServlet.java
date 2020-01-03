package fi.luomus.triplestore.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.annotation.WebServlet;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.json.JSONArray;
import fi.luomus.commons.json.JSONObject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.dao.SearchParams;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.dao.TriplestoreDAO.ResultType;

@WebServlet(urlPatterns = {"/schema/alt/*"})
public class SchemaAltsServlet extends SchemaClassesServlet {

	private static final long serialVersionUID = -7921975069788844624L;

	@Override
	protected ResponseData processGetWithAccess() throws Exception, IOException {
		TriplestoreDAO dao = getTriplestoreDAO();
		Set<Qname> altQnames = getQnamesOfType(dao, "rdf:Alt");
		Collection<Model> models = dao.getSearchDAO().get(altQnames, ResultType.DEEP);
		Map<String, Model> asMap = asMap(models);
		JSONObject response = parseAltsResponse(asMap); 
		return jsonResponse(response);
	}

	private Map<String, Model> asMap(Collection<Model> models) {
		Map<String, Model> map =  new HashMap<>(models.size());
		for (Model m : models) {
			map.put(m.getSubject().getQname(), m);
		}
		return map;
	}

	private Set<Qname> getQnamesOfType(TriplestoreDAO dao, String type) throws Exception {
		return dao.getSearchDAO().searchQnames(new SearchParams(Integer.MAX_VALUE, 0).type(type));
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
	private JSONObject parseAltsResponse(Map<String, Model> models) {
		JSONObject response = new JSONObject();
		for (Model model : models.values()) {
			if ("rdf:Alt".equals(model.getType())) {
				response.setArray(model.getSubject().getQname(), parseAltsResponse(model, models));
			}
		}
		return response;
	}

	private JSONArray parseAltsResponse(Model model, Map<String, Model> models) {
		JSONArray json = new JSONArray();
		List<Statement> orderedStatements = getAltStatementsInOrder(model);
		for (Statement s : orderedStatements) {
			String altId = s.getObjectResource().getQname();
			Model altModel = models.getOrDefault(altId, new Model(new Qname(altId)));
			json.appendObject(parseAlt(altModel));
		}
		return json;
	}

	private List<Statement> getAltStatementsInOrder(Model model) {
		List<Statement> orderedStatements = new ArrayList<>(); 
		for (Statement s : model.getStatements()) {
			if (s.getPredicate().getQname().startsWith("rdf:_") && s.isResourceStatement()) {
				orderedStatements.add(s);
			}
		}
		Collections.sort(orderedStatements, new Comparator<Statement>() {
			@Override
			public int compare(Statement o1, Statement o2) {
				int i1 = order(o1);
				int i2 = order(o2);
				return Integer.compare(i1, i2);
			}
			private Integer order(Statement o1) {
				try {
					return Integer.valueOf(o1.getPredicate().getQname().replace("rdf:_", ""));
				} catch (NumberFormatException e) {
					return Integer.MAX_VALUE;
				}
			}});
		return orderedStatements;
	}

	private JSONObject parseAlt(Model model) {
		JSONObject json = new JSONObject();
		json.setString("id", model.getSubject().getQname());
		if (model.hasStatements("rdfs:label")) {
			json.setObject("value", labels(model));
		}
		for (Statement s : model) {
			if (!s.isForDefaultContext()) continue;
			String predicate = s.getPredicate().getQname(); 
			if (predicate.equals("rdfs:label")) continue;
			if (s.isLiteralStatement()) {
				json.getObject(shortName(predicate)).setString(s.getObjectLiteral().getLangcode(), s.getObjectLiteral().getContent());
			}
		}
		return json;
	}

}
