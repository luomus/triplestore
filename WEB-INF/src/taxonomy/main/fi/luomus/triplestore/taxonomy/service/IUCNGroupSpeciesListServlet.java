package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.containers.InformalTaxonGroup;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.triplestore.taxonomy.models.IUCNEvaluationTarget;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/taxonomy-editor/iucn/group/*"})
public class IUCNGroupSpeciesListServlet extends IUCNFrontpageServlet {

	private static final int DEFAULT_PAGE_SIZE = 50;
	private static final long serialVersionUID = -9070472068743470346L;

	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ResponseData responseData = super.processGet(req, res);
		String groupQname = groupQname(req);
		InformalTaxonGroup group = getTaxonomyDAO().getInformalTaxonGroups().get(groupQname);
		if (group == null) {
			return redirectTo404(res);
		}

		List<IUCNEvaluationTarget> targets = getTaxonomyDAO().getIucnDAO().getIUCNContainer().getTargetsOfGroup(groupQname);

		int pageSize = pageSize(req);
		int currentPage = currentPage(req);
		int pageCount = pageCount(targets.size(), pageSize);
		if (currentPage > pageCount) currentPage = pageCount;

		List<IUCNEvaluationTarget> pageTargets = pageTargets(currentPage, pageSize, targets);

		return responseData.setViewName("iucn-group-species-list")
				.setData("group", group)
				.setData("statusProperty", getTriplestoreDAO().getProperty(new Predicate("MKV.redListStatus")))
				.setData("persons", getTaxonomyDAO().getPersons())
				.setData("targets", pageTargets)
				.setData("currentPage", currentPage)
				.setData("pageCount", pageCount)
				.setData("pageSize", pageSize)
				.setData("defaultPageSize", DEFAULT_PAGE_SIZE);
	}

	private List<IUCNEvaluationTarget> pageTargets(int currentPage, int pageSize, List<IUCNEvaluationTarget> targets) {
		List<IUCNEvaluationTarget> list = new ArrayList<>();
		int offset = (currentPage-1) * pageSize;
		for (int pageItems = 0; pageItems<pageSize; pageItems++) {
			int index = offset + pageItems;
			if (index >= targets.size()) {
				break;
			}
			list.add(targets.get(index));
		}
		return list;
	}

	private int pageCount(int speciesCount, int limit) {
		return (int) Math.ceil( (double) speciesCount / limit);
	}

	private int currentPage(HttpServletRequest req) {
		String currentPage = req.getParameter("page");
		if (!given(currentPage)) return 1;
		try {
			int i = Integer.valueOf(currentPage);
			if (i < 1) return 1;
			return i;
		} catch (Exception e) {
			return 1;
		}
	}

	private int pageSize(HttpServletRequest req) {
		String pageSize = req.getParameter("pageSize");
		if (!given(pageSize)) return DEFAULT_PAGE_SIZE;
		try {
			int i = Integer.valueOf(pageSize);
			if (i < 10) return 10;
			if (i > 5000) return 5000;
			return i;
		} catch (Exception e) {
			return DEFAULT_PAGE_SIZE;
		}
	}


	private String groupQname(HttpServletRequest req) {
		String groupQname = req.getRequestURI().split(Pattern.quote("/group/"))[1].split(Pattern.quote("/"))[0];
		return groupQname;
	}

}
