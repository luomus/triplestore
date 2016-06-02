package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.utils.SingleObjectCache;
import fi.luomus.commons.utils.SingleObjectCache.CacheLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Iterables;

@WebServlet(urlPatterns = {"/taxonomy-editor/iucn", "/taxonomy-editor/iucn/*"})
public class IUCNFrontpageServlet extends TaxonomyEditorBaseServlet {

	private static final long serialVersionUID = 3517803575719451136L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ResponseData responseData = initResponseData(req);

		List<Integer> evaluationYears = getEvaluationYears();
		int draftYear = Iterables.getLast(evaluationYears);
		int selectedYear = selectedYear(req, evaluationYears, draftYear);

		return responseData.setViewName("iucn-frontpage")
				.setData("evaluationYears", evaluationYears)
				.setData("draftYear", draftYear)
				.setData("selectedYear", selectedYear)
				.setData("checklist", getTaxonomyDAO().getChecklists().get("MR.1"))
				.setData("taxonGroups", getTaxonomyDAO().getInformalGroups()); 
	}

	private int selectedYear(HttpServletRequest req, List<Integer> evaluationYears, int draftYear) {
		String selectedYearParam = getId(req);
		if (!given(selectedYearParam)) {
			return draftYear;
		}
		try {
			int selectedYear = Integer.valueOf(selectedYearParam);
			if (!evaluationYears.contains(selectedYear)) {
				return draftYear; 
			}
			return selectedYear;
		} catch (Exception e) {
			return draftYear;
		}
	}

	private final SingleObjectCache<List<Integer>> evaluationYearsCache = new SingleObjectCache<List<Integer>>(new CacheLoader<List<Integer>>() {
		@Override
		public List<Integer> load() {
			List<Integer> evaluationYears = new ArrayList<>();
			try {
				for (Model m : getTriplestoreDAO().getSearchDAO().search("rdf:type", "MKV.iucnRedListEvaluationYear")) {
					int year = Integer.valueOf(m.getStatements("MKV.evaluationYear").get(0).getObjectLiteral().getContent());
					evaluationYears.add(year);
				}
			} catch (Exception e) {
				throw new RuntimeException("Evaluatoin years cache", e);
			}
			Collections.sort(evaluationYears);
			return evaluationYears;
		}
	}, 60*2);

	private List<Integer> getEvaluationYears() throws Exception {
		return evaluationYearsCache.get();
	}

}
