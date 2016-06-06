package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.containers.InformalTaxonGroup;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.taxonomy.models.TaxonGroupIucnEditors;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Iterables;

@WebServlet(urlPatterns = {"/taxonomy-editor/iucn", "/taxonomy-editor/iucn/*"})
public class IUCNFrontpageServlet extends TaxonomyEditorBaseServlet {

	private static final long serialVersionUID = 3517803575719451136L;

	protected static final String IUCN_NAMESPACE = "MKV";

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ResponseData responseData = initResponseData(req);

		List<Integer> evaluationYears = getTaxonomyDAO().getIucnDAO().getEvaluationYears();
		int draftYear = Iterables.getLast(evaluationYears);
		int selectedYear = selectedYear(req, evaluationYears, draftYear);

		Collection<InformalTaxonGroup> groups = getTaxonomyDAO().getInformalTaxonGroups().values();
		Map<String, TaxonGroupIucnEditors> groupEditors = getTaxonomyDAO().getIucnDAO().getGroupEditors();

		return responseData.setViewName("iucn-frontpage")
				.setData("evaluationYears", evaluationYears)
				.setData("draftYear", draftYear)
				.setData("selectedYear", selectedYear)
				.setData("checklist", getTaxonomyDAO().getChecklists().get("MR.1"))
				.setData("taxonGroups", groups)
				.setData("taxonGroupEditors", groupEditors);
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

}
