package fi.luomus.triplestore.taxonomy.iucn.service;

import java.util.List;
import java.util.Map;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Iterables;

import fi.luomus.commons.containers.InformalTaxonGroup;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEditors;
import fi.luomus.triplestore.taxonomy.service.TaxonomyEditorBaseServlet;

@WebServlet(urlPatterns = {"/taxonomy-editor/iucn", "/taxonomy-editor/iucn/*"})
public class FrontpageServlet extends TaxonomyEditorBaseServlet {

	private static final long serialVersionUID = 3517803575719451136L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ResponseData responseData = initResponseData(req);
		int selectedYear = selectedYear(req);
		Map<String, InformalTaxonGroup> groups = getTaxonomyDAO().getInformalTaxonGroups();
		List<Integer> evaluationYears = getTaxonomyDAO().getIucnDAO().getEvaluationYears();
		Map<String, IUCNEditors> groupEditors = getTaxonomyDAO().getIucnDAO().getGroupEditors();
		return responseData.setViewName("iucn-frontpage")
				.setData("evaluationYears", evaluationYears)
				.setData("draftYear", getDraftYear(evaluationYears))
				.setData("selectedYear", selectedYear)
				.setData("checklist", getTaxonomyDAO().getChecklists().get("MR.1"))
				.setData("taxonGroups", groups)
				.setData("taxonGroupEditors", groupEditors);
	}

	protected int selectedYear(HttpServletRequest req) throws Exception {
		String selectedYearParam = getId(req);
		if (!given(selectedYearParam)) {
			return getDraftYear(getTaxonomyDAO().getIucnDAO().getEvaluationYears());
		}
		try {
			int selectedYear = Integer.valueOf(selectedYearParam);
			return selectedYear;
		} catch (Exception e) {
			return getDraftYear(getTaxonomyDAO().getIucnDAO().getEvaluationYears());
		}
	}

	protected int selectedYearFailForNoneGiven(HttpServletRequest req) throws Exception {
		String selectedYearParam = getId(req);
		if (!given(selectedYearParam)) {
			throw new IllegalArgumentException("Must give evaluation year.");
		}
		try {
			int selectedYear = Integer.valueOf(selectedYearParam);
			return selectedYear;
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid evaluation year: " + selectedYearParam);
		}
	}
	
	private int getDraftYear(List<Integer> allYears) throws Exception {
		return Iterables.getLast(allYears);
	}

}
