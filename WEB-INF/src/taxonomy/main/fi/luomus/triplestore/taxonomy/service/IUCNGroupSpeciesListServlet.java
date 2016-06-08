package fi.luomus.triplestore.taxonomy.service;

import fi.luomus.commons.containers.InformalTaxonGroup;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.xml.Document.Node;
import fi.luomus.triplestore.taxonomy.models.IUCNEvaluation;
import fi.luomus.triplestore.taxonomy.models.IUCNEvaluationTarget;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

		String[] states = req.getParameterValues("state");
		String taxon = req.getParameter("taxon");
		String[] redListStatuses = req.getParameterValues("redListStatus"); 
		int selectedYear = (int) responseData.getDatamodel().get("selectedYear");

		List<IUCNEvaluationTarget> filteredTargets;
		try {
			filteredTargets = filter(targets, states, taxon, redListStatuses, selectedYear);
		} catch (TaxonLoadException e) {
			filteredTargets = targets;
			responseData.setData("filterError", "Taksonomiarajaus oli liian laaja.");
		}

		int pageSize = pageSize(req);
		int currentPage = currentPage(req);
		int pageCount = pageCount(filteredTargets.size(), pageSize);
		if (currentPage > pageCount) currentPage = pageCount;

		List<IUCNEvaluationTarget> pageTargets = pageTargets(currentPage, pageSize, filteredTargets);

		return responseData.setViewName("iucn-group-species-list")
				.setData("group", group)
				.setData("statusProperty", getTriplestoreDAO().getProperty(new Predicate("MKV.redListStatus")))
				.setData("persons", getTaxonomyDAO().getPersons())
				.setData("targets", pageTargets)
				.setData("currentPage", currentPage)
				.setData("pageCount", pageCount)
				.setData("pageSize", pageSize)
				.setData("defaultPageSize", DEFAULT_PAGE_SIZE)
				.setData("states", states)
				.setData("taxon", taxon)
				.setData("redListStatuses", redListStatuses);
	}

	private static class TaxonLoadException extends Exception {
		private static final long serialVersionUID = -6749180766121111373L;
	}

	private List<IUCNEvaluationTarget> filter(List<IUCNEvaluationTarget> targets, String[] states, String taxon, String[] redListStatuses, int selectedYear) throws Exception {
		List<IUCNEvaluationTarget> filtered = new ArrayList<>();
		if (!given(states) && !given(taxon) && !given(redListStatuses)) return targets;

		Set<String> taxonQnames = null;
		if (given(taxon)) {
			try {
				taxonQnames = getTaxons(taxon);
			} catch (Exception e) {
				e.printStackTrace();
				throw new TaxonLoadException();
			}
		}

		for (IUCNEvaluationTarget target : targets) {
			IUCNEvaluation evaluation = target.getEvaluation(selectedYear);
			if (given(redListStatuses)) {
				if (!redListStatusesMatch(redListStatuses, evaluation)) continue;
			}
			if (given(states)) {
				if (!statesMatch(states, evaluation)) continue;
			}
			if (given(taxon)) {
				if (!taxonQnames.contains(target.getQname())) continue;
			}
			filtered.add(target);
		}
		return filtered;
	}

	private boolean statesMatch(String[] states, IUCNEvaluation evaluation) {
		for (String state : states) {
			if (!given(state)) continue;
			if (state.equals("ready")) {
				if (evaluation != null && evaluation.isReady()) return true;
			} else if (state.equals("started")) {
				if (evaluation != null && !evaluation.isReady()) return true;
			} else if (state.equals("notStarted")) {
				if (evaluation == null) return true;
			} else {
				throw new UnsupportedOperationException(state);
			}
		}
		return false;
	}

	private boolean redListStatusesMatch(String[] redListStatuses, IUCNEvaluation evaluation) {
		if (evaluation == null) return false;
		String status = evaluation.getIucnStatus();
		if (!given(status)) return false;
		return contains(redListStatuses, status);
	}

	private boolean contains(String[] redListStatuses, String expectedStatus) {
		for (String status : redListStatuses) {
			if (status.equals(expectedStatus)) return true;
		}
		return false;
	}

	private Set<String> getTaxons(String taxon) throws Exception {
		Node result = getTaxonomyDAO().search(taxon, 1000).getRootNode();
		Set<String> qnames = new HashSet<>();
		for (Node exactmatch : result.getChildNodes("exactMatch")) {
			for (Node match : exactmatch.getChildNodes()) {
				qnames.add(match.getName());
			}
		}
		for (String qname : qnames) {
			qnames.addAll(getTaxonomyDAO().getIucnDAO().getFinnishSpecies(qname));
		}
		return qnames;
	}

	private boolean given(String[] values) {
		if (values == null) return false;
		if (values.length == 0) return false;
		for (String v : values) {
			if (given(v)) return true;
		}
		return false;
	}

	private List<IUCNEvaluationTarget> pageTargets(int currentPage, int pageSize, List<IUCNEvaluationTarget> targets) {
		if (targets.isEmpty()) return targets;
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
		if (speciesCount == 0) return 1;
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
