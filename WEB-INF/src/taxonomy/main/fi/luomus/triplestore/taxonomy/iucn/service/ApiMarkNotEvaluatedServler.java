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
import fi.luomus.commons.utils.DateUtils;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.dao.IucnDAO;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNContainer;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluation;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluationTarget;
import fi.luomus.triplestore.taxonomy.service.ApiBaseServlet;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/iucn-mark-not-evaluated/*"})
public class ApiMarkNotEvaluatedServler extends ApiBaseServlet {

	private static final long serialVersionUID = 2710161794053100703L;

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		String speciesQname = req.getParameter("speciesQname");
		String groupQname = req.getParameter("groupQname");
		int year = Integer.valueOf(req.getParameter("year"));
		if (!given(speciesQname)) return redirectTo500(res);
		if (!given(groupQname)) return redirectTo500(res);
		checkIucnPermissions(groupQname, req);
		Qname editor = getUser(req).getQname();

		ExtendedTaxonomyDAO taxonomyDAO = getTaxonomyDAO();
		IucnDAO iucnDAO = taxonomyDAO.getIucnDAO();
		IUCNContainer container = iucnDAO.getIUCNContainer();

		IUCNEvaluation evaluation = createEvaluation(speciesQname, year, editor, iucnDAO, req);
		getTriplestoreDAO(req).store(evaluation, null);
		container.setEvaluation(evaluation);
		IUCNEvaluationTarget target = container.getTarget(evaluation.getSpeciesQname());

		return new ResponseData().setViewName("iucn-species-row-update")
				.setData("evaluation", evaluation)
				.setData("target", target)
				.setData("statusProperty", getTriplestoreDAO().getProperty(new Predicate(IUCNEvaluation.RED_LIST_STATUS)))
				.setData("persons", taxonomyDAO.getPersons())
				.setData("selectedYear", year);
	}

	protected IUCNEvaluation createEvaluation(String speciesQname, int year, Qname editorQname, IucnDAO iucnDAO, HttpServletRequest req) throws Exception {
		IUCNEvaluation evaluation = iucnDAO.createNewEvaluation();
		Model model = evaluation.getModel();
		model.addStatement(new Statement(new Predicate(IUCNEvaluation.EVALUATED_TAXON), new ObjectResource(speciesQname)));
		model.addStatement(new Statement(new Predicate(IUCNEvaluation.EVALUATION_YEAR), new ObjectLiteral(String.valueOf(year))));
		model.addStatement(new Statement(new Predicate(IUCNEvaluation.LAST_MODIFIED), new ObjectLiteral(DateUtils.getCurrentDate())));
		model.addStatement(new Statement(new Predicate(IUCNEvaluation.LAST_MODIFIED_BY), new ObjectResource(editorQname)));
		model.addStatement(new Statement(new Predicate(IUCNEvaluation.RED_LIST_STATUS), new ObjectResource("MX.iucnNE")));
		String notes = IUCNEvaluation.NE_MARK_NOTES + IUCNEvaluation.NOTE_DATE_SEPARATOR + DateUtils.getCurrentDateTime("dd.MM.yyyy"); 
		model.addStatement(new Statement(new Predicate(IUCNEvaluation.EDIT_NOTES), new ObjectLiteral(notes)));
		model.addStatement(new Statement(new Predicate(IUCNEvaluation.STATE), new ObjectResource(IUCNEvaluation.STATE_READY)));
		return evaluation;
	}

}
