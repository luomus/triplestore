package fi.luomus.triplestore.taxonomy.iucn.service;

import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.Area;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;
import fi.luomus.triplestore.taxonomy.dao.IucnDAO;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluation;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluationTarget;

@WebServlet(urlPatterns = {"/taxonomy-editor/iucn/species/*"})
public class EvaluationEditServlet extends FrontpageServlet {

	private static final long serialVersionUID = -9070670126541614377L;

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

		return responseData.setViewName("iucn-evaluation-edit")
				.setData("target", target)
				.setData("taxon", taxon)
				.setData("evaluation", thisPeriodData)
				.setData("comparison", comparisonData)
				.setData("evaluationProperties", dao.getProperties("MKV.iucnRedListEvaluation"))
				.setData("habitatObjectProperties", dao.getProperties("MKV.habitatObject"))
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

}
