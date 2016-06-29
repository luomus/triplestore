package fi.luomus.triplestore.taxonomy.iucn.service;

import fi.luomus.commons.containers.Area;
import fi.luomus.commons.containers.Publication;
import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.ObjectLiteral;
import fi.luomus.commons.containers.rdf.ObjectResource;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.RdfProperties;
import fi.luomus.commons.containers.rdf.RdfProperty;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.taxonomy.Occurrences.Occurrence;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.utils.DateUtils;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.models.UsedAndGivenStatements;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.dao.IucnDAO;
import fi.luomus.triplestore.taxonomy.iucn.model.EditHistory;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluation;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluationTarget;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNHabitatObject;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/taxonomy-editor/iucn/species/*"})
public class EvaluationEditServlet extends FrontpageServlet {

	private static final long serialVersionUID = 8302101785687770794L;

	private static final String EVALUATION_ID = "evaluationId";
	private static final String NEW_IUCN_PUBLICATION_CITATION = "newIucnPublicationCitation";
	private static final Predicate SECONDARY_HABITAT_PREDICATE = new Predicate(IUCNEvaluation.SECONDARY_HABITAT);
	private static final Predicate PRIMARY_HABITAT_PREDICATE = new Predicate(IUCNEvaluation.PRIMARY_HABITAT);
	private static final Predicate HAS_OCCURRENCE_PREDICATE = new Predicate(IUCNEvaluation.HAS_OCCURRENCE);
	private static final Predicate PUBLICATION_PREDICATE = new Predicate(IUCNEvaluation.PUBLICATION);
	private static final Predicate EVALUATION_YEAR_PREDICATE = new Predicate(IUCNEvaluation.EVALUATION_YEAR);
	private static final Predicate EDIT_NOTES_PREDICATE = new Predicate(IUCNEvaluation.EDIT_NOTES);
	private static final Predicate EVALUATED_TAXON_PREDICATE = new Predicate(IUCNEvaluation.EVALUATED_TAXON);

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ResponseData responseData = super.processGet(req, res);
		String speciesQname = speciesQname(req);
		if (!given(speciesQname)) return redirectTo404(res);

		TriplestoreDAO dao = getTriplestoreDAO();
		ExtendedTaxonomyDAO taxonomyDAO = getTaxonomyDAO();
		IucnDAO iucnDAO = taxonomyDAO.getIucnDAO();

		IUCNEvaluationTarget target = iucnDAO.getIUCNContainer().getTarget(speciesQname);

		int year = selectedYear(req);
		IUCNEvaluation comparisonData = getComparisonData(target, year);
		IUCNEvaluation thisPeriodData = target.getEvaluation(year);

		Taxon taxon = taxonomyDAO.getTaxon(new Qname(target.getQname()));

		Map<String, Area> evaluationAreas = iucnDAO.getEvaluationAreas();

		if (thisPeriodData != null) {
			EditHistory editHistory = iucnDAO.getEditHistory(thisPeriodData);
			responseData.setData("editHistory", editHistory);
		}

		return responseData.setViewName("iucn-evaluation-edit")
				.setData("target", target)
				.setData("taxon", taxon)
				.setData("evaluation", thisPeriodData)
				.setData("comparison", comparisonData)
				.setData("evaluationProperties", dao.getProperties(IUCNEvaluation.EVALUATION_CLASS))
				.setData("habitatObjectProperties", dao.getProperties(IUCNEvaluation.HABITAT_OBJECT_CLASS))
				.setData("areas", evaluationAreas)
				.setData("permissions", permissions(req, target));
	}

	private boolean permissions(HttpServletRequest req, IUCNEvaluationTarget target) throws Exception {
		boolean userHasPermissions = false;
		for (String groupQname : target.getGroups()) {
			if (hasIucnPermissions(groupQname, req)) {
				userHasPermissions = true;
			}
		}
		return userHasPermissions;
	}

	private IUCNEvaluation getComparisonData(IUCNEvaluationTarget target, int year) throws Exception {
		Integer comparisonYear = getComparisonYear(year);
		if (comparisonYear == null) return null;
		return target.getEvaluation(comparisonYear);
	}

	private Integer getComparisonYear(int year) throws Exception {
		Integer comparisonYear = null;
		for (Integer evaluationYear : getTaxonomyDAO().getIucnDAO().getEvaluationYears()) {
			if (evaluationYear.equals(year)) {
				return comparisonYear;
			} else {
				comparisonYear = evaluationYear;
			}
		}
		throw new IllegalStateException("Unable to resolve comparison year for "+year);
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

		if (!permissions(req, target)) throw new IllegalAccessException();

		IUCNEvaluation comparisonData = getComparisonData(target, year);
		IUCNEvaluation givenData = buildEvaluation(req, speciesQname, year, dao.getProperties(IUCNEvaluation.EVALUATION_CLASS));

		ValidationResult validationResult = validate(givenData, comparisonData);

		if (!validationResult.hasErrors()) {
			IUCNEvaluation existingEvaluation = target.getEvaluation(year);
			if (existingEvaluation != null) {
				deleteOccurrences(dao, existingEvaluation);
				deleteHabitatObjects(dao, existingEvaluation);
			}

			Model model = givenData.getModel();
			storeOccurrencesAndSetIdToModel(speciesQname, dao, givenData, model);
			storeHabitatObjectsAndSetIdsToModel(iucnDAO, givenData, model);
			storeTaxonProperties(req, speciesQname, dao, taxonomyDAO);

			String newPublicationCitation = req.getParameter(NEW_IUCN_PUBLICATION_CITATION);
			if (given(newPublicationCitation)) {
				insertPublicationAndSetToModel(dao, model, newPublicationCitation);
				taxonomyDAO.getPublicationsForceReload();
			}

			setEditNotes(givenData);

			dao.store(model);
			iucnDAO.getIUCNContainer().setEvaluation(givenData);
		}

		setFlashMessage(req, givenData, validationResult);
		return redirectTo(getConfig().baseURL() + "/iucn/species/" + speciesQname + "/" + year , res);
	}

	private boolean invalidState(String state) {
		return !IUCNEvaluation.STATE_READY.equals(state) && !IUCNEvaluation.STATE_STARTED.equals(state);
	}

	private void storeTaxonProperties(HttpServletRequest req, String speciesQname, TriplestoreDAO dao, ExtendedTaxonomyDAO taxonomyDAO) throws Exception {
		RdfProperties taxonProperties = dao.getProperties("MX.taxon");
		boolean hadTaxonProperties = false;
		for (Map.Entry<String, String[]> e : req.getParameterMap().entrySet()) {
			String parameterName = e.getKey();
			if (!parameterName.startsWith("MX.")) continue;
			for (String value : e.getValue()) {
				storeTaxonProperties(speciesQname, dao, taxonProperties, parameterName, value);
				hadTaxonProperties = true;
			}
		}
		if (hadTaxonProperties) {
			EditableTaxon taxon = (EditableTaxon) taxonomyDAO.getTaxon(new Qname(speciesQname));
			taxon.invalidate();
		}
	}

	private void setFlashMessage(HttpServletRequest req, IUCNEvaluation givenData, ValidationResult validationResult) {
		if (validationResult.hasErrors()) {
			getSession(req).setFlashError(validationResult.getErrors());
		} else {
			if (givenData.isReady()) {
				getSession(req).setFlashSuccess("Tallennettu ja merkitty valmiiksi!");
			} else {
				getSession(req).setFlashSuccess("Tallennettu onnistuneesti!");
			}
		}
	}

	private void storeHabitatObjectsAndSetIdsToModel(IucnDAO iucnDAO, IUCNEvaluation givenData, Model model) throws Exception {
		IUCNHabitatObject primaryHabitat = givenData.getPrimaryHabitat();
		if (primaryHabitat != null) {
			iucnDAO.store(primaryHabitat);
			model.addStatement(new Statement(PRIMARY_HABITAT_PREDICATE, new ObjectResource(primaryHabitat.getId())));
		}
		for (IUCNHabitatObject secondaryHabitat : givenData.getSecondaryHabitats()) {
			iucnDAO.store(secondaryHabitat);
			model.addStatement(new Statement(PRIMARY_HABITAT_PREDICATE, new ObjectResource(secondaryHabitat.getId())));
		}
	}

	private void storeOccurrencesAndSetIdToModel(String speciesQname, TriplestoreDAO dao, IUCNEvaluation givenData, Model model) throws Exception {
		for (Occurrence occurrence : givenData.getOccurrences()) {
			dao.store(new Qname(speciesQname), occurrence);
			model.addStatement(new Statement(HAS_OCCURRENCE_PREDICATE, new ObjectResource(occurrence.getId())));
		}
	}

	private void deleteHabitatObjects(TriplestoreDAO dao, IUCNEvaluation existingEvaluation) throws Exception {
		if (existingEvaluation.getPrimaryHabitat() != null) {
			dao.delete(new Subject(existingEvaluation.getPrimaryHabitat().getId()));
		}
		for (IUCNHabitatObject habitat : existingEvaluation.getSecondaryHabitats()) {
			dao.delete(new Subject(habitat.getId()));
		}
	}

	private void deleteOccurrences(TriplestoreDAO dao, IUCNEvaluation existingEvaluation) throws Exception {
		for (Occurrence occurrence : existingEvaluation.getOccurrences()) {
			dao.delete(new Subject(occurrence.getId()));
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
		for (Map.Entry<String, Map<String, String[]>> e : habitatPairParameters.entrySet()) {
			String predicateAndIndex = e.getKey();
			String habitat = null;
			if (e.getValue().containsKey(IUCNEvaluation.HABITAT)) {
				habitat = e.getValue().get(IUCNEvaluation.HABITAT)[0];
			}
			String[] habitatSpecificTypes = e.getValue().get(IUCNEvaluation.HABITAT_SPECIFIC_TYPE);
			IUCNHabitatObject habitatObject = new IUCNHabitatObject(null, habitat);
			if (habitatSpecificTypes != null) {
				for (String type : habitatSpecificTypes) {
					if (given(type)) {
						habitatObject.addHabitatSpecificType(type);
					}
				}
			}
			if (habitatObject.hasValues()) {
				if (predicateAndIndex.startsWith(PRIMARY_HABITAT_PREDICATE.getQname())) {
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
		return parameterName.startsWith(PRIMARY_HABITAT_PREDICATE.getQname()) || parameterName.startsWith(SECONDARY_HABITAT_PREDICATE.getQname());
	}

	private void setEditNotes(IUCNEvaluation givenData) {
		String notes = givenData.isReady() ? "Merkitty valmiiksi" : "Tallennettu";
		notes += " " + DateUtils.getCurrentDateTime("dd.MM.yyyy"); 
		Model model = givenData.getModel();
		if (model.hasStatements(EDIT_NOTES_PREDICATE.getQname())) {
			notes += ": " + model.getStatements(EDIT_NOTES_PREDICATE.getQname()).get(0).getObjectLiteral().getContent();
		}
		model.removeAll(EDIT_NOTES_PREDICATE);
		model.addStatement(new Statement(EDIT_NOTES_PREDICATE, new ObjectLiteral(notes)));
	}

	private void setYear(int year, Model model) {
		model.removeAll(EVALUATION_YEAR_PREDICATE);
		model.addStatement(new Statement(EVALUATION_YEAR_PREDICATE, new ObjectLiteral(String.valueOf(year))));
	}

	private void setTaxon(String speciesQname, Model model) {
		model.removeAll(EVALUATED_TAXON_PREDICATE);
		model.addStatement(new Statement(EVALUATED_TAXON_PREDICATE, new ObjectResource(speciesQname)));
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
		if (parameterName.startsWith(HAS_OCCURRENCE_PREDICATE.getQname())) {
			// MKV.hasOccurrence___ML.xxx
			String areaQname = parameterName.split(Pattern.quote("___"))[1];
			evaluation.addOccurrence(new Occurrence(null, new Qname(areaQname), new Qname(value)));
			return;
		}
		setToModel(evaluation.getModel(), iucnProperties, parameterName, value);
	}

	private void setToModel(Model model, RdfProperties iucnProperties, String parameterName, String value) {
		RdfProperty property = iucnProperties.getProperty(parameterName);
		if (property.isLiteralProperty()) {
			model.addStatement(new Statement(new Predicate(parameterName), new ObjectLiteral(value)));
		} else {
			model.addStatement(new Statement(new Predicate(parameterName), new ObjectResource(value)));
		}
	}

	private void insertPublicationAndSetToModel(TriplestoreDAO dao, Model model, String citation) throws Exception {
		Publication publication = new Publication(dao.getSeqNextValAndAddResource("MP"));
		publication.setCitation(citation);
		dao.storePublication(publication);
		model.addStatement(new Statement(PUBLICATION_PREDICATE, new ObjectResource(publication.getQname())));
	}

	private void storeTaxonProperties(String speciesQname, TriplestoreDAO dao, RdfProperties taxonProperties, String parameterName, String value) throws Exception {
		UsedAndGivenStatements usedAndGivenStatements = new UsedAndGivenStatements();
		Predicate predicate = new Predicate(parameterName);
		usedAndGivenStatements.addUsed(predicate, null, null);
		if (taxonProperties.getProperty(predicate).isLiteralProperty()) {
			usedAndGivenStatements.addStatement(new Statement(predicate, new ObjectLiteral(value)));
		} else {
			usedAndGivenStatements.addStatement(new Statement(predicate, new ObjectResource(value)));
		}
		// XXX disabloitu toistaiseksi: dao.store(new Subject(speciesQname), usedAndGivenStatements);
	}

	private ValidationResult validate(IUCNEvaluation givenData, IUCNEvaluation comparisonData) {
		ValidationResult validationResult = new ValidationResult();
		// TODO Auto-generated method stub
		if ("foo".equals("bar")) {
			validationResult.setError("blaablaa");
		}
		return validationResult;
	}

	private static class ValidationResult {
		private final List<String> errors = new ArrayList<>();
		public boolean hasErrors() {
			return !errors.isEmpty();
		}
		public void setError(String errorMessage) {
			errors.add(errorMessage);
		}
		public String getErrors() {
			StringBuilder b = new StringBuilder();
			for (String error : errors) {
				b.append("<p>").append(error).append("</p>");
			}
			return b.toString();
		}
	}

}
