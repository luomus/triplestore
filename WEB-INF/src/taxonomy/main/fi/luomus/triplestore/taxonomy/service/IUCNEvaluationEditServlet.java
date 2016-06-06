package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.triplestore.taxonomy.models.IUCNEvaluation;
import fi.luomus.triplestore.taxonomy.models.IUCNEvaluationTarget;

import java.util.regex.Pattern;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/taxonomy-editor/iucn/species/*"})
public class IUCNEvaluationEditServlet extends IUCNFrontpageServlet {

	private static final long serialVersionUID = -9070670126541614377L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ResponseData responseData = super.processGet(req, res);
		String speciesQname = speciesQname(req);
		IUCNEvaluationTarget target = getTaxonomyDAO().getIucnDAO().getIUCNContainer().getTarget(speciesQname);
		int year = selectedYear(req);
		IUCNEvaluation comparisonData = getComparisonData(target, year);
		IUCNEvaluation thisPeriodData = target.getEvaluation(year);
		Taxon taxon = getTaxonomyDAO().getTaxon(new Qname(target.getQname()));
		return responseData.setViewName("iucn-evaluation-edit")
				.setData("target", target)
				.setData("taxon", taxon)
				.setData("evaluation", thisPeriodData)
				.setData("comparison", comparisonData)
				.setData("properties", getTriplestoreDAO().getProperties("MKV.iucnRedListEvaluation"));
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
		String speciesQname = req.getRequestURI().split(Pattern.quote("/species/"))[1].split(Pattern.quote("/"))[0];
		return speciesQname;
	}

}
