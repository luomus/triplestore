package fi.luomus.triplestore.taxonomy.iucn.service;

import fi.luomus.commons.containers.InformalTaxonGroup;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.session.SessionHandler;
import fi.luomus.commons.taxonomy.TaxonomyDAO.TaxonSearch;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNContainer;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluation;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluationTarget;
import fi.luomus.triplestore.taxonomy.models.TaxonSearchResponse;
import fi.luomus.triplestore.taxonomy.models.TaxonSearchResponse.Match;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/taxonomy-editor/iucn/group/*"})
public class GroupSpeciesListServlet extends FrontpageServlet {

	private static final String PAGE_SIZE = "pageSize";
	private static final String ORDER_BY = "orderBy";
	private static final String TAXON = "taxon";
	private static final String STATE = "state";
	private static final String RED_LIST_STATUS = "redListStatus";
	private static final int DEFAULT_PAGE_SIZE = 100;
	private static final long serialVersionUID = -9070472068743470346L;

	private static final Comparator<IUCNEvaluationTarget> ALPHA_COMPARATOR = new Comparator<IUCNEvaluationTarget>() {
		@Override
		public int compare(IUCNEvaluationTarget o1, IUCNEvaluationTarget o2) {
			String s1 = o1.getTaxon().getScientificName();
			String s2 = o2.getTaxon().getScientificName();
			if (s1 == null) s1 = "\uffff'";
			if (s2 == null) s2 = "\uffff'";
			return s1.compareTo(s2);
		}}; 
	
	@Override
	protected ResponseData processGet(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ResponseData responseData = super.processGet(req, res);
		String groupQname = groupQname(req);
		InformalTaxonGroup group = getTaxonomyDAO().getInformalTaxonGroups().get(groupQname);
		if (group == null) {
			return redirectTo404(res);
		}

		IUCNContainer container = getTaxonomyDAO().getIucnDAO().getIUCNContainer();
		List<IUCNEvaluationTarget> targets = container.getTargetsOfGroup(groupQname);

		SessionHandler session = getSession(req);

		String clearFilters = req.getParameter("clearFilters");
		String taxon = req.getParameter(TAXON);
		String[] states = req.getParameterValues(STATE);
		String[] redListStatuses = req.getParameterValues(RED_LIST_STATUS); 
		int selectedYear = (int) responseData.getDatamodel().get("selectedYear");
		String orderBy = req.getParameter(ORDER_BY);
		Integer pageSize = pageSize(req);

		if (orderBy == null) {
			orderBy = session.get(ORDER_BY);
		}
		
		if (!"true".equals(clearFilters) && !given(states) && !given(taxon) && !given(redListStatuses)) {
			taxon = session.get(TAXON);
			states = (String[]) session.getObject(STATE);
			redListStatuses = (String[]) session.getObject(RED_LIST_STATUS);
			
		}
		if (pageSize == null) {
			pageSize = (Integer) session.getObject(PAGE_SIZE);
			if (pageSize == null) {
				pageSize = DEFAULT_PAGE_SIZE;
			}
		}
		
		List<IUCNEvaluationTarget> filteredTargets;
		try {
			filteredTargets = filter(targets, states, taxon, redListStatuses, selectedYear);
		} catch (TaxonLoadException e) {
			filteredTargets = targets;
			responseData.setData("filterError", "Taksonomiarajaus oli liian laaja.");
		}
		
		if ("alphabetic".equals(orderBy)) {
			List<IUCNEvaluationTarget> sorted = new ArrayList<>(filteredTargets);
			Collections.sort(sorted, ALPHA_COMPARATOR);
			filteredTargets = sorted;
		}
		
		session.put(TAXON, taxon);
		session.setObject(STATE, states);
		session.setObject(RED_LIST_STATUS, redListStatuses);
		session.setObject(ORDER_BY, orderBy);
		session.setObject(PAGE_SIZE, pageSize);
		
		int currentPage = currentPage(req);
		int pageCount = pageCount(filteredTargets.size(), pageSize);
		if (currentPage > pageCount) currentPage = pageCount;

		List<IUCNEvaluationTarget> pageTargets = pageTargets(currentPage, pageSize, filteredTargets);

		return responseData.setViewName("iucn-group-species-list")
				.setData("group", group)
				.setData("statusProperty", getTriplestoreDAO().getProperty(new Predicate(IUCNEvaluation.RED_LIST_STATUS)))
				.setData("persons", getTaxonomyDAO().getPersons())
				.setData("targets", pageTargets)
				.setData("remarks", container.getRemarksForGroup(groupQname))
				.setData("currentPage", currentPage)
				.setData("pageCount", pageCount)
				.setData(PAGE_SIZE, pageSize)
				.setData("defaultPageSize", DEFAULT_PAGE_SIZE)
				.setData("states", states)
				.setData(TAXON, taxon)
				.setData("redListStatuses", redListStatuses)
				.setData("permissions", hasIucnPermissions(groupQname, req))
				.setData(ORDER_BY, orderBy);
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
		TaxonSearchResponse result = getTaxonomyDAO().searchInternal(new TaxonSearch(taxon, 1000).onlyExact());
		Set<String> qnames = new HashSet<>();
		for (Match exactmatch : result.getExactMatches()) {
			qnames.add(exactmatch.getTaxon().getQname().toString());
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

	private Integer pageSize(HttpServletRequest req) {
		String pageSize = req.getParameter(PAGE_SIZE);
		if (!given(pageSize)) return null;
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
