package fi.luomus.triplestore.taxonomy.iucn.service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.Publication;
import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.ObjectLiteral;
import fi.luomus.commons.containers.rdf.ObjectResource;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.RdfProperties;
import fi.luomus.commons.containers.rdf.RdfProperty;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.taxonomy.Occurrences.Occurrence;
import fi.luomus.commons.taxonomy.TaxonomyDAO;
import fi.luomus.commons.utils.DateUtils;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.dao.IucnDAO;
import fi.luomus.triplestore.taxonomy.iucn.model.EditHistory;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEndangermentObject;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluation;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluationTarget;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNHabitatObject;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNValidationResult;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNValidator;

@WebServlet(urlPatterns = {"/taxonomy-editor/iucn/species/*"})
public class EvaluationEditServlet extends FrontpageServlet {

	private static final long serialVersionUID = 8302101785687770794L;

	private static final String EVALUATION_ID = "evaluationId";
	private static final String NEW_IUCN_PUBLICATION_CITATION = "newIucnPublicationCitation";

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		String speciesQname = speciesQname(req);
		if (!given(speciesQname)) return redirectTo404(res);

		TriplestoreDAO dao = getTriplestoreDAO(req);
		ExtendedTaxonomyDAO taxonomyDAO = getTaxonomyDAO();
		IucnDAO iucnDAO = taxonomyDAO.getIucnDAO();

		IUCNEvaluationTarget target = iucnDAO.getIUCNContainer().getTarget(speciesQname);

		int year = selectedYear(req);
		IUCNEvaluation comparisonData = target.getPreviousEvaluation(year);
		IUCNEvaluation thisPeriodData = target.getEvaluation(year);
		complete(comparisonData);
		complete(thisPeriodData);
		if (isCopyRequest(req) && thisPeriodData == null && comparisonData != null) {
			thisPeriodData = iucnDAO.createNewEvaluation();
			comparisonData.copySpecifiedFieldsTo(thisPeriodData);
			
			Model model = thisPeriodData.getModel();
			setModifiedInfo(req, model);
			setTaxon(speciesQname, model);
			setYear(year, model);
			String notes = "Vuoden " + comparisonData.getEvaluationYear() + " tiedot kopioitu" + IUCNEvaluation.NOTE_DATE_SEPARATOR + DateUtils.getCurrentDateTime("dd.MM.yyyy"); 
			model.addStatement(new Statement(IucnDAO.EDIT_NOTES_PREDICATE, new ObjectLiteral(notes)));
			
			return storeAndRedirectToGet(req, res, speciesQname, year, dao, taxonomyDAO, iucnDAO, target, thisPeriodData, new IUCNValidationResult());
		}
		return showView(req, res, dao, taxonomyDAO, iucnDAO, target, comparisonData, thisPeriodData);
	}

	private void complete(IUCNEvaluation evaluation) throws Exception {
		if (evaluation == null) return;
		if (evaluation.isIncompletelyLoaded()) {
			getTaxonomyDAO().getIucnDAO().getIUCNContainer().complateLoading(evaluation);
		}
	}

	private boolean isCopyRequest(HttpServletRequest req) {
		String copyParam = req.getParameter("copy");
		return copyParam != null && copyParam.equals("true");
	}

	private ResponseData showView(HttpServletRequest req, HttpServletResponse res, TriplestoreDAO dao, TaxonomyDAO taxonomyDAO, IucnDAO iucnDAO, IUCNEvaluationTarget target, IUCNEvaluation comparisonData, IUCNEvaluation thisPeriodData) throws Exception {
		ResponseData responseData = super.processGet(req, res);

		if (thisPeriodData != null) {
			EditHistory editHistory = iucnDAO.getEditHistory(thisPeriodData);
			responseData.setData("editHistory", editHistory);
		}

		return responseData.setViewName("iucn-evaluation-edit")
				.setData("target", target)
				.setData("taxon", taxonomyDAO.getTaxon(new Qname(target.getQname())))
				.setData("permissions", permissions(req, target, thisPeriodData))
				.setData("redListIndexPermissions", permissions(req, target, null))
				.setData("evaluation", thisPeriodData)
				.setData("comparison", comparisonData)
				.setData("habitatLabelIndentator", getHabitatLabelIndentaror());
	}

	protected boolean permissions(HttpServletRequest req, IUCNEvaluationTarget target, IUCNEvaluation thisPeriodData) throws Exception {
		boolean userHasPermissions = false;
		for (String groupQname : target.getGroups()) {
			if (hasIucnPermissions(groupQname, req)) {
				userHasPermissions = true;
			}
		}
		if (!userHasPermissions) return false;
		if (thisPeriodData == null) return true;
		return !thisPeriodData.isLocked();
	}

	private String speciesQname(HttpServletRequest req) {
		try {
			String speciesQname = req.getRequestURI().split(Pattern.quote("/species/"))[1].split(Pattern.quote("/"))[0];
			return speciesQname;
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		String speciesQname = speciesQname(req);
		if (!given(speciesQname)) throw new IllegalArgumentException("Species qname not given.");

		int year = selectedYearFailForNoneGiven(req);

		String state = req.getParameter(IUCNEvaluation.STATE);
		if (invalidState(state)) throw new IllegalArgumentException("Invalid state: " + state);

		TriplestoreDAO dao = getTriplestoreDAO(req);
		ExtendedTaxonomyDAO taxonomyDAO = getTaxonomyDAO();
		IucnDAO iucnDAO = taxonomyDAO.getIucnDAO();
		IUCNEvaluationTarget target = iucnDAO.getIUCNContainer().getTarget(speciesQname);

		if (!permissions(req, target, target.getEvaluation(year))) throw new IllegalAccessException();

		IUCNEvaluation comparisonData = target.getPreviousEvaluation(year);
		complete(comparisonData);
		IUCNEvaluation givenData = buildEvaluation(req, speciesQname, year, dao.getProperties(IUCNEvaluation.EVALUATION_CLASS));
		cleanCriteriaFormats(givenData);
		
		IUCNValidationResult validationResult = new IUCNValidator(dao, getErrorReporter()).validate(givenData, comparisonData);

		if (!validationResult.hasErrors()) {
			return storeAndRedirectToGet(req, res, speciesQname, year, dao, taxonomyDAO, iucnDAO, target, givenData, validationResult);
		}

		givenData.getModel().removeAll(IucnDAO.EDIT_NOTES_PREDICATE);

		return showView(req, res, dao, taxonomyDAO, iucnDAO, target, comparisonData, givenData)
				.setData("errorMessage", validationResult.getErrors())
				.setData("erroreousFields", validationResult.getErroreousFields())
				.setData("editNotes", req.getParameter(IUCNEvaluation.EDIT_NOTES));
	}

	private void cleanCriteriaFormats(IUCNEvaluation givenData) {
		String criteriaForStatus = givenData.getValue(IUCNEvaluation.CRITERIA_FOR_STATUS);
		cleanAndReplaceCriteria(criteriaForStatus, IUCNEvaluation.CRITERIA_FOR_STATUS, givenData);
		for (String criteriaPrefix : IUCNEvaluation.CRITERIAS) {
			String criteria = givenData.getValue("MKV.criteria"+criteriaPrefix);
			cleanAndReplaceCriteria(criteria, "MKV.criteria"+criteriaPrefix, givenData);
		}
	}

	private void cleanAndReplaceCriteria(String criteria, String predicateQname, IUCNEvaluation givenData) {
		if (!given(criteria)) return;
		String clanedCriteria = cleanCriteria(criteria);
		Predicate p = new Predicate(predicateQname);
		givenData.getModel().removeAll(p);
		givenData.getModel().addStatementIfObjectGiven(p, clanedCriteria);
	}

	private String cleanCriteria(String criteria) {
		criteria = Utils.removeWhitespace(criteria);
		criteria = criteria.replace(";", "; ");
		return criteria;
	}

	private ResponseData storeAndRedirectToGet(HttpServletRequest req, HttpServletResponse res, String speciesQname, int year, TriplestoreDAO dao, ExtendedTaxonomyDAO taxonomyDAO, IucnDAO iucnDAO, IUCNEvaluationTarget target, IUCNEvaluation givenData, IUCNValidationResult validationResult) throws Exception {
		IUCNEvaluation existingEvaluation = target.getEvaluation(year);
		
		String newPublicationCitation = req.getParameter(NEW_IUCN_PUBLICATION_CITATION);
		if (given(newPublicationCitation)) {
			insertPublicationAndSetToModel(dao, givenData, newPublicationCitation);
			taxonomyDAO.getPublicationsForceReload();
		}

		setEditNotes(givenData);
		setRemarks(givenData, existingEvaluation);
		
		dao.store(givenData, existingEvaluation);
		
		iucnDAO.getIUCNContainer().setEvaluation(givenData);

		setFlashMessage(req, givenData, validationResult);
		return redirectTo(getConfig().baseURL() + "/iucn/species/" + speciesQname + "/" + year , res);
	}

	private void setRemarks(IUCNEvaluation givenData, IUCNEvaluation existingEvaluation) {
		for (Statement s : existingEvaluation.getRemarkSatements()) {
			givenData.getModel().addStatement(s);
		}
	}

	private boolean invalidState(String state) {
		return !IUCNEvaluation.STATE_READY.equals(state) && !IUCNEvaluation.STATE_STARTED.equals(state) && !IUCNEvaluation.STATE_READY_FOR_COMMENTS.equals(state);
	}

	private void setFlashMessage(HttpServletRequest req, IUCNEvaluation givenData, IUCNValidationResult validationResult) {
		if (isCopyRequest(req)) {
			getSession(req).setFlashSuccess("Kopioitu onnistuneesti!");
		} else if (givenData.isReady()) {
			getSession(req).setFlashSuccess("Tallennettu ja merkitty valmiiksi!");
		} else if (givenData.isReadyForComments()) {
			getSession(req).setFlashSuccess("Tallennettu ja valmis kommentoitavaksi!");
		} else {
			getSession(req).setFlashSuccess("Tallennettu onnistuneesti!");
		}
	}

	private IUCNEvaluation buildEvaluation(HttpServletRequest req, String speciesQname, int year, RdfProperties iucnProperties) throws Exception {
		IUCNEvaluation evaluation = createEvaluationWithExistingIdOrNewId(req);

		Model model = evaluation.getModel();
		setModifiedInfo(req, model);
		setTaxon(speciesQname, model);
		setYear(year, model);

		Map<String, Map<String, String[]>> habitatPairParameters = new HashMap<>();
		for (Map.Entry<String, String[]> e : req.getParameterMap().entrySet()) {
			String parameterName = e.getKey();
			if (habitatPair(parameterName)) {
				addHabitatPairParameters(habitatPairParameters, e);
			} else {
				setValues(evaluation, iucnProperties, e);
			}
		}

		setHabitatsToEvaluation(evaluation, habitatPairParameters);

		return evaluation;
	}

	private void setModifiedInfo(HttpServletRequest req, Model model) {
		model.addStatement(new Statement(new Predicate(IUCNEvaluation.LAST_MODIFIED), new ObjectLiteral(DateUtils.getCurrentDate())));
		model.addStatement(new Statement(new Predicate(IUCNEvaluation.LAST_MODIFIED_BY), new ObjectResource(getUser(req).getQname())));
	}

	private void setHabitatsToEvaluation(IUCNEvaluation evaluation, Map<String, Map<String, String[]>> habitatPairParameters) {
		// Map of
		// MKV.primaryHabitat___0 : { MKV.habitat: [MKV.habitatMk], MKV.habitatSpecificType : [MKV.habitatSpecificTypePAK] }
		// MKV.secondaryHabitat___0: { MKV.habitat : [MKV.habitatMk] , ... } 
		// MKV.primaryHabitat___1 : ...
		for (Map.Entry<String, Map<String, String[]>> e : habitatPairParameters.entrySet()) {
			String[] predicateAndIndexParts = e.getKey().split(Pattern.quote("___"));
			String predicate = predicateAndIndexParts[0];
			int order = Integer.valueOf(predicateAndIndexParts[1]);
			Qname habitat = null;
			if (e.getValue().containsKey(IUCNEvaluation.HABITAT)) {
				habitat = new Qname(e.getValue().get(IUCNEvaluation.HABITAT)[0]);
			}
			String[] habitatSpecificTypes = e.getValue().get(IUCNEvaluation.HABITAT_SPECIFIC_TYPE);
			IUCNHabitatObject habitatObject = new IUCNHabitatObject(null, habitat, order);
			if (habitatSpecificTypes != null) {
				for (String type : habitatSpecificTypes) {
					if (given(type)) {
						habitatObject.addHabitatSpecificType(new Qname(type));
					}
				}
			}
			if (habitatObject.hasValues()) {
				if (predicate.equals(IUCNEvaluation.PRIMARY_HABITAT)) {
					evaluation.setPrimaryHabitat(habitatObject);
				} else {
					evaluation.addSecondaryHabitat(habitatObject);
				}
			}
		}
	}

	private void addHabitatPairParameters(Map<String, Map<String, String[]>> habitatPairs, Map.Entry<String, String[]> e) {
		// MKV.primaryHabitat___0___MKV.habitat : MKV.habitatMk
		// MKV.primaryHabitat___0___MKV.habitatSpecificType : MKV.habitatSpecificTypePAK
		// MKV.secondaryHabitat___0___MKV.habitat : MKV.habitatMk
		// MKV.secondaryHabitat___0___MKV.habitatSpecificType
		// MKV.secondaryHabitat___1___MKV.habitat : MKV.habitatSr
		String[] nameparts = e.getKey().split(Pattern.quote("___"));
		String predicate = nameparts[0];
		String index = nameparts[1];
		String pairPredicate = nameparts[2];
		String commonPart = predicate + "___" + index;
		if (!habitatPairs.containsKey(commonPart)) {
			habitatPairs.put(commonPart, new HashMap<String, String[]>());
		}
		habitatPairs.get(commonPart).put(pairPredicate, e.getValue());
	}

	private boolean habitatPair(String parameterName) {
		return parameterName.startsWith(IUCNEvaluation.PRIMARY_HABITAT) || parameterName.startsWith(IUCNEvaluation.SECONDARY_HABITAT);
	}

	private void setEditNotes(IUCNEvaluation givenData) {
		String notes = givenData.isReady() ? "Merkitty valmiiksi" : givenData.isReadyForComments() ? "Valmis kommentoitavaksi" : "Tallennettu";
		Model model = givenData.getModel();
		if (model.hasStatements(IucnDAO.EDIT_NOTES_PREDICATE.getQname())) {
			notes += ": " + model.getStatements(IucnDAO.EDIT_NOTES_PREDICATE.getQname()).get(0).getObjectLiteral().getContent();
		}
		notes += IUCNEvaluation.NOTE_DATE_SEPARATOR + DateUtils.getCurrentDateTime("dd.MM.yyyy");  
		model.removeAll(IucnDAO.EDIT_NOTES_PREDICATE);
		model.addStatement(new Statement(IucnDAO.EDIT_NOTES_PREDICATE, new ObjectLiteral(notes)));
	}

	private void setYear(int year, Model model) {
		model.removeAll(IucnDAO.EVALUATION_YEAR_PREDICATE);
		model.addStatement(new Statement(IucnDAO.EVALUATION_YEAR_PREDICATE, new ObjectLiteral(String.valueOf(year))));
	}

	private void setTaxon(String speciesQname, Model model) {
		model.removeAll(IucnDAO.EVALUATED_TAXON_PREDICATE);
		model.addStatement(new Statement(IucnDAO.EVALUATED_TAXON_PREDICATE, new ObjectResource(speciesQname)));
	}

	private IUCNEvaluation createEvaluationWithExistingIdOrNewId(HttpServletRequest req) throws Exception {
		String evaluationId = req.getParameter(EVALUATION_ID);
		if (given(evaluationId)) {
			return getTaxonomyDAO().getIucnDAO().createEvaluation(evaluationId);
		} 
		return getTaxonomyDAO().getIucnDAO().createNewEvaluation();
	}

	private void setValues(IUCNEvaluation evaluation, RdfProperties iucnProperties, Map.Entry<String, String[]> e) {
		String parameterName = e.getKey();
		if (parameterName.equals(EVALUATION_ID)) return;
		if (parameterName.startsWith("MX.")) return;
		if (parameterName.equals(NEW_IUCN_PUBLICATION_CITATION)) return;

		String[] values = e.getValue();
		if (values == null) return;

		for (String value : values) {
			if (!given(value)) continue;
			setValue(evaluation, iucnProperties, parameterName, value);
		}
	}

	private void setValue(IUCNEvaluation evaluation, RdfProperties iucnProperties, String parameterName, String value) {
		if (parameterName.startsWith(IUCNEvaluation.HAS_OCCURRENCE)) {
			// MKV.hasOccurrence___ML.xxx___status
			// MKV.hasOccurrence___ML.xxx___threatened
			String areaQname = splitAreaQname(parameterName);
			String field = splitField(parameterName);
			if (evaluation.hasOccurrence(areaQname)) {
				if (field.equals("status")) {
					evaluation.getOccurrence(areaQname).setStatus(new Qname(value));
				} else if ("RT".equals(value)) {
					evaluation.getOccurrence(areaQname).setThreatened(true);
				}
			} else {
				if (field.equals("status")) {
					evaluation.addOccurrence(new Occurrence(null, new Qname(areaQname), new Qname(value)));
				} else if ("RT".equals(value)){
					Occurrence o = new Occurrence(null, new Qname(areaQname), null);
					o.setThreatened(true);
					evaluation.addOccurrence(o);
				}
			}
			return;
		}
		if (parameterName.startsWith(IUCNEvaluation.HAS_ENDANGERMENT_REASON)) {
			// MKV.hasEndangermentReason___1   <2,3...>
			int order = splitOrder(parameterName);
			evaluation.addEndangermentReason(new IUCNEndangermentObject(null, new Qname(value), order));
			return;
		}
		if (parameterName.startsWith(IUCNEvaluation.HAS_THREAT)) {
			// MKV.hasThreat___1   <2,3...>
			int order = splitOrder(parameterName);
			evaluation.addThreat(new IUCNEndangermentObject(null, new Qname(value), order));
			return;
		}
		setToModel(evaluation.getModel(), iucnProperties, parameterName, value);
	}

	private int splitOrder(String parameterName) {
		int order = Integer.valueOf(parameterName.split(Pattern.quote("___"))[1]);
		return order;
	}

	private String splitField(String parameterName) {
		return parameterName.split(Pattern.quote("___"))[2];
	}
	
	private String splitAreaQname(String parameterName) {
		return parameterName.split(Pattern.quote("___"))[1];
	}

	private void setToModel(Model model, RdfProperties iucnProperties, String parameterName, String value) {
		RdfProperty property = iucnProperties.getProperty(parameterName);
		if (property.isLiteralProperty()) {
			if (property.isDecimalProperty()) {
				value = value.replace(",", ".");
				if (value.startsWith(".")) value = "0" + value;
			}
			model.addStatement(new Statement(new Predicate(parameterName), new ObjectLiteral(value)));
		} else {
			model.addStatement(new Statement(new Predicate(parameterName), new ObjectResource(value)));
		}
	}

	private void insertPublicationAndSetToModel(TriplestoreDAO dao, IUCNEvaluation givenData, String citation) throws Exception {
		Publication publication = new Publication(null);
		publication.setCitation(citation);
		dao.storePublication(publication);
		givenData.getModel().addStatement(new Statement(IucnDAO.PUBLICATION_PREDICATE, new ObjectResource(publication.getQname())));
	}
	
}
