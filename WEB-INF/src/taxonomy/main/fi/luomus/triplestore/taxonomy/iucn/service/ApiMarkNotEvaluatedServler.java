package fi.luomus.triplestore.taxonomy.iucn.service;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.ObjectLiteral;
import fi.luomus.commons.containers.rdf.ObjectResource;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.taxonomy.iucn.Evaluation;
import fi.luomus.commons.utils.DateUtils;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.dao.IucnDAO;
import fi.luomus.triplestore.taxonomy.iucn.model.Container;
import fi.luomus.triplestore.taxonomy.iucn.model.EvaluationTarget;
import fi.luomus.triplestore.taxonomy.iucn.model.ValidationResult;
import fi.luomus.triplestore.taxonomy.iucn.model.Validator;
import fi.luomus.triplestore.taxonomy.service.ApiBaseServlet;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/iucn-mark-not-evaluated/*"})
public class ApiMarkNotEvaluatedServler extends ApiBaseServlet {

	private static final long serialVersionUID = 2710161794053100703L;

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		log(req);
		String speciesQname = req.getParameter("speciesQname");
		String groupQname = req.getParameter("groupQname");
		int year = Integer.valueOf(req.getParameter("year"));
		if (!given(speciesQname)) return status500(res);
		if (!given(groupQname)) return status500(res);
		checkIucnPermissions(groupQname, req);
		Qname editor = getUser(req).getQname();

		ExtendedTaxonomyDAO taxonomyDAO = getTaxonomyDAO();
		IucnDAO iucnDAO = taxonomyDAO.getIucnDAO();
		Container container = iucnDAO.getIUCNContainer();

		if (container.getTarget(speciesQname).getEvaluation(year) != null) {
			getErrorReporter().report("Using fast-eval button for target that already has evaluation: " + Utils.debugS(speciesQname, year));
			return status(422, res);
		}
		
		Evaluation evaluation = createEvaluation(speciesQname, year, editor, iucnDAO, req);
		
		ValidationResult validationResult = new Validator(getTriplestoreDAO(), getErrorReporter()).validate(evaluation, null);

		if (validationResult.hasErrors()) {
			return status(422, res);
		}
		
		getTriplestoreDAO(req).store(evaluation, null);
		container.setEvaluation(evaluation);
		EvaluationTarget target = container.getTarget(evaluation.getSpeciesQname());

		return new ResponseData().setViewName("iucn-species-row-update")
				.setData("evaluation", evaluation)
				.setData("target", target)
				.setData("statusProperty", getTriplestoreDAO().getProperty(Predicate.of(Evaluation.RED_LIST_STATUS)))
				.setData("persons", taxonomyDAO.getPersons())
				.setData("selectedYear", year)
				.setData("permissions", true)
				.setData("draftYear", getDraftYear());
	}

	protected Evaluation createEvaluation(String speciesQname, int year, Qname editorQname, IucnDAO iucnDAO, @SuppressWarnings("unused") HttpServletRequest req) throws Exception {
		Evaluation evaluation = iucnDAO.createNewEvaluation();
		Model model = evaluation.getModel();
		model.addStatement(new Statement(Predicate.of(Evaluation.EVALUATED_TAXON), ObjectResource.of(speciesQname)));
		model.addStatement(new Statement(Predicate.of(Evaluation.EVALUATION_YEAR), new ObjectLiteral(String.valueOf(year))));
		model.addStatement(new Statement(Predicate.of(Evaluation.LAST_MODIFIED), new ObjectLiteral(DateUtils.getCurrentDate())));
		model.addStatement(new Statement(Predicate.of(Evaluation.LAST_MODIFIED_BY), ObjectResource.of(editorQname)));
		model.addStatement(new Statement(Predicate.of(Evaluation.RED_LIST_STATUS), ObjectResource.of(Evaluation.NE)));
		String notes = Evaluation.NE_MARK_NOTES + Evaluation.NOTE_DATE_SEPARATOR + DateUtils.getCurrentDateTime("dd.MM.yyyy");
		model.addStatement(new Statement(Predicate.of(Evaluation.EDIT_NOTES), new ObjectLiteral(notes)));
		model.addStatement(new Statement(Predicate.of(Evaluation.STATE), ObjectResource.of(Evaluation.STATE_READY)));
		return evaluation;
	}

}
