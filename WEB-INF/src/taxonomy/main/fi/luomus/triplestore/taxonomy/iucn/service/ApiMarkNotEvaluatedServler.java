package fi.luomus.triplestore.taxonomy.iucn.service;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.services.ResponseData;
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

		IUCNEvaluation evaluation = container.markNotEvaluated(speciesQname, year, editor);
		IUCNEvaluationTarget target = container.getTarget(evaluation.getSpeciesQname());

		return new ResponseData().setViewName("iucn-species-row-update")
				.setData("evaluation", evaluation)
				.setData("target", target)
				.setData("statusProperty", getTriplestoreDAO().getProperty(new Predicate("MKV.redListStatus")))
				.setData("persons", taxonomyDAO.getPersons())
				.setData("selectedYear", year);
	}

}
