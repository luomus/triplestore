package fi.luomus.triplestore.taxonomy.iucn.service;

import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.ObjectLiteral;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.taxonomy.iucn.Evaluation;
import fi.luomus.commons.utils.DateUtils;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.dao.IucnDAO;
import fi.luomus.triplestore.taxonomy.iucn.model.EvaluationTarget;
import fi.luomus.triplestore.taxonomy.iucn.model.ValidationResult;
import fi.luomus.triplestore.taxonomy.iucn.model.Validator;

@WebServlet(urlPatterns = {"/taxonomy-editor/iucn/regional/*"})
public class RegionalEvaluationEditServlet extends EvaluationEditServlet {

	private static final long serialVersionUID = -2751994998798123485L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return super.processGet(req, res).setViewName("iucn-regional-edit");
	}

	@Override
	protected boolean permissions(HttpServletRequest req, EvaluationTarget target, Evaluation thisPeriodData) throws Exception {
		if (thisPeriodData == null) return false;
		boolean userHasPermissions = false;
		for (String groupQname : target.getGroups()) {
			if (hasIucnPermissions(groupQname, req)) {
				userHasPermissions = true;
			}
		}
		if (!userHasPermissions) return false;
		return true;
	}

	@Override
	protected String speciesQname(HttpServletRequest req) {
		try {
			String speciesQname = req.getRequestURI().split(Pattern.quote("/regional/"))[1].split(Pattern.quote("/"))[0];
			return speciesQname;
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		log(req);
		String speciesQname = speciesQname(req);
		if (!given(speciesQname)) throw new IllegalArgumentException("Species qname not given.");

		int year = selectedYearFailForNoneGiven(req);

		TriplestoreDAO dao = getTriplestoreDAO(req);
		ExtendedTaxonomyDAO taxonomyDAO = getTaxonomyDAO();
		IucnDAO iucnDAO = taxonomyDAO.getIucnDAO();
		EvaluationTarget target = iucnDAO.getIUCNContainer().getTarget(speciesQname);

		if (!permissions(req, target, target.getEvaluation(year))) throw new IllegalAccessException();

		Evaluation existingEvaluation = target.getEvaluation(year);
		Model existingModelCopy = dao.get(existingEvaluation.getId());
		Evaluation modifiedEvaluation = iucnDAO.createEvaluation(existingModelCopy);
		existingModelCopy.removeAll(IucnDAO.HAS_OCCURRENCE_PREDICATE);
		existingModelCopy.removeAll(new Predicate("MKV.regionallyThreatenedNotes"));
		iucnDAO.completeLoading(modifiedEvaluation);

		for (Map.Entry<String, String[]> e : req.getParameterMap().entrySet()) {
			setValues(modifiedEvaluation, iucnDAO.getEvaluationProperties(), e); 
		}

		ValidationResult validationResult = new Validator(dao, getErrorReporter()).validate(modifiedEvaluation, null);

		if (!validationResult.hasErrors()) {
			return storeRegionalEvaluationAndRedirectToGet(req, dao, iucnDAO, existingEvaluation, modifiedEvaluation);
		}

		Evaluation comparisonData = target.getPreviousEvaluation(year);
		complete(comparisonData);

		return showView(req, res, taxonomyDAO, iucnDAO, target, comparisonData, existingEvaluation)
				.setData("errorMessage", validationResult.getErrors())
				.setData("erroreousFields", validationResult.getErroreousFields())
				.setViewName("iucn-regional-edit");
	}

	private ResponseData storeRegionalEvaluationAndRedirectToGet(HttpServletRequest req, TriplestoreDAO dao, IucnDAO iucnDAO, Evaluation existingEvaluation, Evaluation modifiedEvaluation) throws Exception {
		setEditNotes(modifiedEvaluation);
		setModifiedInfo(req, modifiedEvaluation.getModel());

		dao.storeOnlyOccurrences(modifiedEvaluation, existingEvaluation);
		iucnDAO.getIUCNContainer().setEvaluation(modifiedEvaluation);

		getSession(req).setFlashSuccess("Alueellinen uhanalaisuus tallennettu!");
		return redirectTo(getConfig().baseURL() + "/iucn/regional/" + modifiedEvaluation.getSpeciesQname() + "/" + modifiedEvaluation.getEvaluationYear());
	}

	private void setEditNotes(Evaluation givenData) {
		String notes = "Alueellinen uhanalaisuus täydennetty";
		notes += Evaluation.NOTE_DATE_SEPARATOR + DateUtils.getCurrentDateTime("dd.MM.yyyy");  
		Model model = givenData.getModel();
		model.removeAll(IucnDAO.EDIT_NOTES_PREDICATE);
		model.addStatement(new Statement(IucnDAO.EDIT_NOTES_PREDICATE, new ObjectLiteral(notes)));
	}

}
