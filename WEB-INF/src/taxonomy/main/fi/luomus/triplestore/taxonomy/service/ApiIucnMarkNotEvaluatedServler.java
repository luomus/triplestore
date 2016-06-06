package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.taxonomy.models.TaxonGroupIucnEvaluationData.EvaluationYearData;
import fi.luomus.triplestore.taxonomy.models.TaxonGroupIucnEvaluationData.EvaluationYearSpeciesData;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/taxonomy-editor/api/iucn-mark-not-evaluated/*"})
public class ApiIucnMarkNotEvaluatedServler extends ApiBaseServlet {

	private static final long serialVersionUID = 2710161794053100703L;

	@Override
	protected ResponseData processPost(HttpServletRequest req, HttpServletResponse res) throws Exception {
		String speciesQname = req.getParameter("speciesQname");
		String groupQname = req.getParameter("groupQname");
		int year = Integer.valueOf(req.getParameter("year"));
		if (!given(speciesQname)) return redirectTo500(res);
		if (!given(groupQname)) return redirectTo500(res);
		
		EvaluationYearData evaluationYearData = getTaxonomyDAO().getIucnDAO().getTaxonGroupData(groupQname).getYear(year);
		EvaluationYearSpeciesData yearSpeciesData = evaluationYearData.markNotEvaluated(speciesQname, getUser(req).getQname(), getTriplestoreDAO());
		return new ResponseData().setViewName("iucn-species-row-update")
				.setData("data", yearSpeciesData)
				.setData("statusProperty", getTriplestoreDAO().getProperty(new Predicate("MKV.redListStatus")))
				.setData("persons", getTaxonomyDAO().getPersons());
	}

}
