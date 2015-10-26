package fi.luomus.triplestore.service;

import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.RdfProperties;
import fi.luomus.commons.containers.rdf.RdfProperty;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.json.JSONObject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.utils.DateUtils;
import fi.luomus.commons.utils.LogUtils;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.models.History;
import fi.luomus.triplestore.models.Model;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/editor/*"})
public class TriplestoreEditorServlet extends EditorBaseServlet {

	private static final long serialVersionUID = 6027569298204324891L;

	@Override
	protected boolean authorized(HttpServletRequest req) {
		if (!super.authorized(req)) return false;
		return getUser(req).isAdmin();
	}

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ResponseData responseData = super.initResponseData(req);

		History history = (History) getSession(req).getObject("history");
		if (history == null) {
			history = new History();
			getSession(req).setObject("history", history);
		}
		responseData.setData("history", history);
		
		String qname = getQname(req);
		if (!given(qname)) {
			return mainPage(responseData);
		}
		
		if (isTaxon(qname)) {
			responseData.setData("error", "Taxons must be edited using Taxon editor.");
			return mainPage(responseData);
		}

		TriplestoreDAO dao = getTriplestoreDAO();

		if (!dao.resourceExists(qname)) {
			responseData.setData("error", "Resource " +qname + " does not exist.");
			return mainPage(responseData);
		}

		Model model = dao.get(qname);
		history.visited(qname);

		if (model.hasStatementsFromNonDefaultContext()) {
			responseData.setData("error", "Resource " +qname + " has statements from some context. This editor does not support editing context's yet!");
			return mainPage(responseData);
		}

		RdfProperties properties = getProperties(dao, model);

		addDefaultProperty("rdfs:label", "xsd:string", properties);
		addDefaultProperty("rdfs:comment", "xsd:string", properties);
		addDefaultProperty("rdfs:range", null, properties);
		addDefaultProperty("sortOrder", "xsd:integer", properties);
		addDefaultProperty("rdf:type", null, properties);
		addDefaultProperty("rdfs:domain", null, properties);

		for (Statement s : model.getStatements()) {
			if (!properties.hasProperty(s.getPredicate().getQname())) {
				properties.addProperty(dao.getProperty(s.getPredicate()));
			} 
		}

		responseData.setData("modelRdfXml", model.getRDF());
		
		return responseData.setViewName("edit").setData("model", model).setData("properties", properties);
	}

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		String qname = getQname(req);
		try {
			return saveAndRedirect(req, qname);
		} catch (Exception e) {
			getSession(req).setFlashError(LogUtils.buildStackTrace(e, 10));
			if (given(qname)) {
				return redirectToGet(qname);
			} else {
				return redirectTo(getConfig().baseURL()+"/editor", res);
			}
		}
	}

	private ResponseData saveAndRedirect(HttpServletRequest req, String qname) throws IllegalAccessException, Exception {
		if (!given(qname)) throw new IllegalArgumentException("Qname not given!");

		TriplestoreDAO dao = getTriplestoreDAO(req);
		if (!dao.resourceExists(qname)) {
			throw new IllegalArgumentException("Resource " + qname + " does not exist");
		}

		JSONObject data = new JSONObject(req.getParameter("data"));

		Model model = dao.get(qname);

		if (model.hasStatementsFromNonDefaultContext()) {
			getSession(req).setFlashError("Resource " +qname + " has statements from some context. This editor does not support editing context's yet!");
			return redirectToGet(qname);
		}

		model = toModel(qname, data);
		JSONObject validationResponse = validate(model, dao);
		if (validationResponse.getBoolean("hasErrors")) {
			throw new IllegalStateException("Validation failed: " + validationResponse.toString());
		}

		dao.store(model);

		getSession(req).setFlashSuccess("Saved!");

		return redirectToGet(qname);
	}

	private RdfProperty getPropertyForStatement(RdfProperties properties, Statement s, TriplestoreDAO dao) throws Exception {
		if (properties.hasProperty(s.getPredicate().getQname())) {
			return properties.getProperty(s.getPredicate());
		} else {
			return dao.getProperty(s.getPredicate());	
		}
	}

	protected JSONObject validate(Model model, TriplestoreDAO dao) throws Exception {
		RdfProperties properties = getProperties(dao, model);
		JSONObject validationResponse = new JSONObject();
		validationResponse.setBoolean("hasErrors", false);

		for (Statement s : model.getStatements()) {
			RdfProperty p = getPropertyForStatement(properties, s, dao);
			if (s.isResourceStatement()) {
				validateResourceStatement(s, p, dao, validationResponse);
			} else {
				validateLiteralStatement(s, p, dao, validationResponse);
			}
		}

		return validationResponse;
	}

	private void validateLiteralStatement(Statement s, RdfProperty p, TriplestoreDAO dao, JSONObject validationResponse) throws Exception {
		if (!p.isLiteralProperty()) {
			if (p.hasRange()) {
				validationError("Objectliteral given for " + s.getPredicate().getQname() + ", which range is " + p.getRange().getQname(), validationResponse);
			} else {
				validationError("Objectliteral given for " + s.getPredicate().getQname() + ", which range is not defined to be a literal", validationResponse);
			}
			return;
		}
		String value = s.getObjectLiteral().getContent();

		if (p.isBooleanProperty()) {
			if (!"true".equals(value) && !"false".equals(value)) {
				validationError("Invalid boolean value for " + s.getPredicate().getQname(), validationResponse);
			}
			return;
		}

		if (p.isDateProperty()) {
			validateDate(value, validationResponse);
			return;
		}

		if (p.isIntegerProperty()) {
			validateInteger(value, validationResponse);
			return;
		}
		if (p.isDecimalProperty()) {
			validateDecimal(value, validationResponse);
			return;
		}
	}

	private void validateDecimal(String value, JSONObject validationResponse) {
		try {
			Double.valueOf(value);
		} catch (NumberFormatException e) {
			validationError("Invalid decimal: " + value, validationResponse);
		}
	}

	private void validateInteger(String value, JSONObject validationResponse) {
		try {
			Integer.valueOf(value);
		} catch (NumberFormatException e) {
			validationError("Invalid integer: " + value, validationResponse);
		}
	}

	private void validateDate(String value, JSONObject validationResponse) {
		if (!DateUtils.validIsoDateTime(value)) {
			validationError("Invalid date: " + value, validationResponse);
		}
	}

	private void validateResourceStatement(Statement s, RdfProperty p, TriplestoreDAO dao, JSONObject validationResponse) throws Exception {
		if (p.isLiteralProperty()) {
			validationError("Objectresource given for " + s.getPredicate().getQname() + ", which range is " + p.getRange().getQname(), validationResponse);
			return;
		}
		Qname resourceQname = Qname.fromURI(s.getObjectResource().getURI());
		if (!dao.resourceExists(resourceQname)) {
			validationError("Unknown resource " + s.getObjectResource().getQname(), validationResponse);
			return;
		}
		if (p.hasRangeValues()) {
			if (!resourceValueIsRangeValue(p, resourceQname)) {
				validationError("Value " + resourceQname + " for predicate " + s.getPredicate().getQname() + " is not among defined values " + p.getRange().getQname(), validationResponse);
			}
		} else if (p.hasRange()) {
			Qname range = p.getRange().getQname();
			Model value = dao.get(resourceQname);
			if (!value.getType().equals(range.toString())) {
				validationError("Value " + resourceQname + " should be of type " + range + " but it is " + value.getType(), validationResponse);
			}
		}
	}

	private boolean resourceValueIsRangeValue(RdfProperty p, Qname resourceQname) {
		boolean found = false;
		for (RdfProperty rangeValue : p.getRange().getValues()) {
			if (rangeValue.getQname().equals(resourceQname)) found = true;
		}
		return found;
	}

	private void validationError(String errorMessage, JSONObject validationResponse) {
		validationResponse.setBoolean("hasErrors", true);
		validationResponse.getArray("errors").appendString(errorMessage);
	}

	protected Model toModel(String qname, JSONObject data) {
		Model model;
		model = new Model(new Subject(qname));
		for (JSONObject predicateData : data.getArray("predicates").iterateAsObject()) {
			String predicateQname = predicateData.getString("predicate");
			if (!given(predicateQname)) throw new IllegalArgumentException("Empty predicate given.");
			Predicate predicate = new Predicate(predicateQname);
			if (predicateData.hasKey("objectResource")) {
				model.addStatementIfObjectGiven(predicate, new Qname(predicateData.getString("objectResource")));
			} else if (predicateData.hasKey("objectLiteral")) {
				model.addStatementIfObjectGiven(predicate, predicateData.getString("objectLiteral"), predicateData.getString("langcode"));
			}
		}
		return model;
	}

	protected ResponseData redirectToGet(String qname) {
		return new ResponseData().setRedirectLocation(getConfig().baseURL()+"/editor/"+qname);
	}

	private boolean isTaxon(String qname) {
		if (!qname.startsWith("MX.")) return false;
		qname = qname.replace("MX.", "");
		return allNumbers(qname);
	}

	private boolean allNumbers(String qnamePart) {
		try {
			Integer.valueOf(qnamePart);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private void addDefaultProperty(String predicate, String range, RdfProperties properties) {
		if (properties.hasProperty(predicate)) return;
		Qname rangeQname = range == null ? null : new Qname(range);
		properties.addProperty(new RdfProperty(new Qname(predicate), rangeQname));
	}

	private RdfProperties getProperties(TriplestoreDAO dao, Model model) throws Exception {
		String rdfType = model.getType();
		if (rdfType != null) {
			return dao.getProperties(rdfType);
		} else {
			return new RdfProperties();
		}
	}

	protected ResponseData mainPage(ResponseData responseData) throws Exception {
		return responseData.setViewName("main").setData("resources", getTriplestoreDAO().getResourceStats());
	}
}
