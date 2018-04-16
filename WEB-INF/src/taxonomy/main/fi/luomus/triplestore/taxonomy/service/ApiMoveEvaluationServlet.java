package fi.luomus.triplestore.taxonomy.service;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluationTarget;
import fi.luomus.triplestore.taxonomy.models.EditableTaxon;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/moveEvaluation/*"})
public class ApiMoveEvaluationServlet extends ApiBaseServlet {

	private static final long serialVersionUID = -39032381120274355L;

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		boolean delete = "delete".equals(req.getParameter("action"));
		
		String taxonId = req.getParameter("taxonID");
		String newTargetId = req.getParameter("newTargetID");
		List<Integer> years = yearsToMove(req);

		if (!given(taxonId)) {
			return error("Taxon to move from is missing");
		}
		if (years.isEmpty()) {
			return error("No years selected");
		}

		if (delete) {
			return delete(taxonId, years, req, res);
		}
		
		if (!given(newTargetId)) {
			return error("Target taxon info is missing");
		}

		TriplestoreDAO dao = getTriplestoreDAO(req);
		ExtendedTaxonomyDAO taxonomyDAO = getTaxonomyDAO();
		EditableTaxon taxon = (EditableTaxon) taxonomyDAO.getTaxon(new Qname(taxonId));
		EditableTaxon targetTaxon = (EditableTaxon) taxonomyDAO.getTaxon(new Qname(newTargetId));

		try {
			checkPermissionsToAlterTaxon(taxon, req);
			checkPermissionsToAlterTaxon(targetTaxon, req);
		} catch (IllegalAccessException e) {
			return error(e.getMessage());
		}

		List<Statement> iucnStatementsToMove = new ArrayList<>();
		Model sourceModel = dao.get(taxon.getQname());
		Model destinationModel = dao.get(targetTaxon.getQname());
		IUCNEvaluationTarget iucnSource = taxonomyDAO.getIucnDAO().getIUCNContainer().getTarget(taxonId);
		IUCNEvaluationTarget iucnTarget = taxonomyDAO.getIucnDAO().getIUCNContainer().getTarget(newTargetId);

		for (int year : years) {
			if (iucnTarget.hasEvaluation(year)) {
				return error("Target taxon already has IUCN evaluation for year " + year);
			}
			String predicateName = "MX.redListStatus"+year+"Finland";
			if (sourceModel.hasStatements(predicateName)) {
				iucnStatementsToMove.add(sourceModel.getStatements(predicateName).iterator().next());
				if (destinationModel.hasStatements(predicateName)) {
					return error("Target taxon already has IUCN status for year " + year);
				}
			}
		}

		for (Statement s : iucnStatementsToMove) {
			dao.deleteStatement(s.getId());
			dao.insert(new Subject(targetTaxon.getQname()), s);
		}
		for (int year : years) {
			if (iucnSource.hasEvaluation(year)) {
				taxonomyDAO.getIucnDAO().moveEvaluation(taxonId, newTargetId, year);
			}
		}
		
		targetTaxon.invalidateSelf();
		taxon.invalidateSelf();
		
		return apiSuccessResponse(res);
	}

	private ResponseData delete(String taxonId, List<Integer> years, HttpServletRequest req, HttpServletResponse res) throws Exception {
		if (!getUser(req).isAdmin()) return error("Must be admin");
		
		ExtendedTaxonomyDAO taxonomyDAO = getTaxonomyDAO();
		EditableTaxon taxon = (EditableTaxon) taxonomyDAO.getTaxon(new Qname(taxonId));
		TriplestoreDAO dao = getTriplestoreDAO();
		Model sourceModel = dao.get(taxonId);
		
		for (int year : years) {
			String predicateName = "MX.redListStatus"+year+"Finland";
			for (Statement s : sourceModel.getStatements(predicateName)) {
				dao.deleteStatement(s.getId());
			}
			taxonomyDAO.getIucnDAO().deleteEvaluation(taxonId, year);
		}
		
		taxon.invalidateSelf();
		
		return apiSuccessResponse(res);
	}

	private ResponseData error(String message) {
		return new ResponseData().setData("error", message).setViewName("api-error");
	}

	private List<Integer> yearsToMove(HttpServletRequest req) {
		List<Integer> years = new ArrayList<>();
		if (req.getParameter("evaluationYears") == null) return years;
		for (String s : req.getParameterValues("evaluationYears")) {
			years.add(Integer.valueOf(s));
		}
		return years;
	}

}
