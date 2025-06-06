package fi.luomus.triplestore.taxonomy.iucn.service;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.luomus.commons.containers.Area;
import fi.luomus.commons.containers.InformalTaxonGroup;
import fi.luomus.commons.containers.Person;
import fi.luomus.commons.containers.RedListEvaluationGroup;
import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.RdfProperties;
import fi.luomus.commons.containers.rdf.RdfProperty;
import fi.luomus.commons.services.ResponseData;
import fi.luomus.commons.session.SessionHandler;
import fi.luomus.commons.taxonomy.Occurrences.Occurrence;
import fi.luomus.commons.taxonomy.TaxonSearch;
import fi.luomus.commons.taxonomy.TaxonSearchResponse;
import fi.luomus.commons.taxonomy.TaxonSearchResponse.Match;
import fi.luomus.commons.taxonomy.iucn.EndangermentObject;
import fi.luomus.commons.taxonomy.iucn.Evaluation;
import fi.luomus.commons.taxonomy.iucn.HabitatObject;
import fi.luomus.commons.utils.DateUtils;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.iucn.model.Container;
import fi.luomus.triplestore.taxonomy.iucn.model.EvaluationTarget;
import fi.luomus.triplestore.taxonomy.iucn.model.EvaluationYear;

@WebServlet(urlPatterns = {"/taxonomy-editor/iucn/group/*"})
public class GroupSpeciesListServlet extends FrontpageServlet {

	private static final String MISSING_VALUE = "--";
	private static final String PAGE_SIZE = "pageSize";
	private static final String ORDER_BY = "orderBy";
	private static final String TAXON = "taxon";
	private static final String STATE = "state";
	private static final String RED_LIST_STATUS = "redListStatus";
	private static final String PREV_RED_LIST_STATUS = "prevRedListStatus";
	private static final int DEFAULT_PAGE_SIZE = 100;
	private static final long serialVersionUID = -9070472068743470346L;
	private static final List<String> CRITERIAS = Utils.list("A", "B", "C", "D", "E");

	private static final Comparator<EvaluationTarget> ALPHA_COMPARATOR = new Comparator<EvaluationTarget>() {
		@Override
		public int compare(EvaluationTarget o1, EvaluationTarget o2) {
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
				return status404(res);
			}

			Container container = getTaxonomyDAO().getIucnDAO().getIUCNContainer();
			List<EvaluationTarget> targets = container.getTargetsOfGroup(groupQname);

			SessionHandler session = getSession(req);

			String clearFilters = req.getParameter("clearFilters");
			String taxon = req.getParameter(TAXON);
			String[] states = req.getParameterValues(STATE);
			String[] redListStatuses = req.getParameterValues(RED_LIST_STATUS);
			String[] prevRedListStatuses = req.getParameterValues(PREV_RED_LIST_STATUS);
			int selectedYear = (int) responseData.getDatamodel().get("selectedYear");
			String orderBy = req.getParameter(ORDER_BY);
			Integer pageSize = pageSize(req);

			if (orderBy == null) {
				orderBy = session.get(ORDER_BY);
			}

			if (!"true".equals(clearFilters) && !given(states) && !given(taxon) && !given(redListStatuses) && !given(prevRedListStatuses)) {
				taxon = session.get(TAXON);
				states = (String[]) session.getObject(STATE);
				redListStatuses = (String[]) session.getObject(RED_LIST_STATUS);
				prevRedListStatuses = (String[]) session.getObject(PREV_RED_LIST_STATUS);
			}

			if (pageSize == null) {
				pageSize = (Integer) session.getObject(PAGE_SIZE);
				if (pageSize == null) {
					pageSize = DEFAULT_PAGE_SIZE;
				}
			}

			List<EvaluationTarget> filteredTargets;
			try {
				filteredTargets = filter(targets, states, taxon, redListStatuses, prevRedListStatuses, selectedYear);
			} catch (TaxonLoadException e) {
				filteredTargets = targets;
				responseData.setData("filterError", "Taksonomiarajaus oli liian laaja.");
			}

			if ("alphabetic".equals(orderBy)) {
				List<EvaluationTarget> sorted = new ArrayList<>(filteredTargets);
				Collections.sort(sorted, ALPHA_COMPARATOR);
				filteredTargets = sorted;
			}

			session.put(TAXON, taxon);
			session.setObject(STATE, states);
			session.setObject(RED_LIST_STATUS, redListStatuses);
			session.setObject(PREV_RED_LIST_STATUS, prevRedListStatuses);
			session.setObject(ORDER_BY, orderBy);
			session.setObject(PAGE_SIZE, pageSize);

			int currentPage = currentPage(req);
			int pageCount = pageCount(filteredTargets.size(), pageSize);
			if (currentPage > pageCount) currentPage = pageCount;

			TriplestoreDAO dao = getTriplestoreDAO();
			if (isFileDownload(req)) {
				return doDownload(res, container, selectedYear, filteredTargets);
			}
			List<EvaluationTarget> pageTargets = isFileDownload(req) ? filteredTargets : pageTargets(currentPage, pageSize, filteredTargets);
			return responseData.setViewName("iucn-group-species-list")
					.setData("group", group)
					.setData("statusProperty", dao.getProperty(new Predicate(Evaluation.RED_LIST_STATUS)))
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
					.setData("prevRedListStatuses", prevRedListStatuses)
					.setData("permissions", hasIucnPermissions(groupQname, req))
					.setData(ORDER_BY, orderBy)
					.setData("evaluationProperties", getTaxonomyDAO().getIucnDAO().getEvaluationProperties())
					.setData("habitatObjectProperties", dao.getProperties(Evaluation.HABITAT_OBJECT_CLASS))
					.setData("occurrenceStatuses", getOccurrenceStatuses())
					.setData("habitatLabelIndentator", getHabitatLabelIndentaror());
		}

		private ResponseData doDownload(HttpServletResponse res, Container container, int selectedYear, Collection<EvaluationTarget> targets) throws Exception {
			List<String> rows = getDownloadRows(container, selectedYear, targets);
			writeFileDownloadRows(res, selectedYear, rows);
			return new ResponseData().setOutputAlreadyPrinted();
		}

		protected List<String> getDownloadRows(Container container, int selectedYear, Collection<EvaluationTarget> targets) throws Exception {
			List<Integer> years = new ArrayList<>();
			for (EvaluationYear e : getTaxonomyDAO().getIucnDAO().getEvaluationYears()) {
				years.add(e.getYear());
			}
			Collections.reverse(years);
			List<String> rows = new ArrayList<>(targets.size()*4);
			rows.add(tsv(fileDownloadHeaderRow(selectedYear, years)));
			appendDownloadDataRows(container, selectedYear, targets, years, rows);
			return rows;
		}

		private void writeFileDownloadRows(HttpServletResponse res, int selectedYear, List<String> rows) throws IOException {
			res.setHeader("Content-disposition","attachment; filename=IUCN_" + selectedYear + "_" + DateUtils.getFilenameDatetime() + ".tsv");
			res.setContentType("text/tab-separated-values; charset=utf-8");
			PrintWriter writer = res.getWriter();
			int i = 0;
			for (String row : rows) {
				writer.write(row);
				writer.write("\n");
				if (i++ % 500 == 0) writer.flush();
			}
			writer.flush();
		}

		private void appendDownloadDataRows(Container container, int selectedYear, Collection<EvaluationTarget> targets, List<Integer> years, List<String> rows) throws Exception {
			int i = 0;
			for (EvaluationTarget target : targets) {
				if (i % 1000 == 0) System.out.println(i + "/" + targets.size());
				i++;
				if (target.hasEvaluation(selectedYear)) {
					Evaluation evaluation = target.getEvaluation(selectedYear);
					if (evaluation.isIncompletelyLoaded()) {
						container.complateLoading(evaluation);
					}

					rows.add(tsv(fileDownloadDataRow(evaluation, target, years)));
				} else {
					rows.add(tsv(fileDownloadDataRow(new Evaluation(new Model(new Qname("foo")), getIUCNProperties()), target, years)));
				}
			}
		}

		private String tsv(List<String> data) {
			return Utils.toTSV(data);
		}

		private List<String> fileDownloadDataRow(Evaluation evaluation, EvaluationTarget target, List<Integer> years) throws Exception {
			int selectedYear = evaluation.getEvaluationYear() == null ? years.get(0) : evaluation.getEvaluationYear();
			Evaluation previous = target.getPreviousEvaluation(selectedYear);
			List<String> data = new ArrayList<>();
			List<RedListEvaluationGroup> targetGroups = getGroups(target);
			RedListEvaluationGroup groupRoot = getGroupRoot(target);
			RedListEvaluationGroup group2 = null;
			RedListEvaluationGroup group3 = null;
			if (groupRoot !=  null) {
				group2 = getSubGroup(groupRoot, targetGroups);
				if (group2 != null) {
					group3 = getSubGroup(group2, targetGroups);
				}
			}
			data.add(groupName(groupRoot));
			data.add(groupName(group2));
			data.add(groupName(group3));
			data.add(target.getQname());
			data.add(target.getOrderAndFamily());
			data.add(taxonRank(target));
			data.add(target.getScientificName());
			data.add(target.getTaxon().getScientificNameAuthorship());
			data.add(target.getSynonymNames());
			data.add(target.getTaxon().getVernacularName().forLocale("fi"));
			data.add(target.getTaxon().getVernacularName().forLocale("sv"));
			data.add(target.getTaxon().getVernacularName().forLocale("en"));
			data.add(target.getTaxon().getNotes());
			data.add(adminStatuses(target));
			data.add(state(evaluation));
			data.add(lastModified(evaluation));
			data.add(lastModifiedBy(evaluation));
			data.add(statusWithSymbols(evaluation));
			if (previous == null) {
				data.add(MISSING_VALUE);
				data.add(MISSING_VALUE);
			} else {
				data.add(statusWithSymbols(previous) + " (" + previous.getEvaluationYear()+")");
				String corrected = previous.getCorrectedStatusForRedListIndex();
				if (corrected != null) {
					data.add(status(corrected) + " (" + previous.getEvaluationYear()+")");
				} else {
					data.add(MISSING_VALUE);
				}
			}
			data.add(evaluation.getRemarks());
			data.add(" -> ");
			data.add(v(evaluation, "MKV.taxonomicNotes"));
			data.add(enumValue(evaluation, "MKV.typeOfOccurrenceInFinland", getOccurrenceStatuses()));
			data.add(v(evaluation, "MKV.typeOfOccurrenceInFinlandNotes"));

			data.add(pair(evaluation, "MKV.distributionAreaMin", "MKV.distributionAreaMax"));
			data.add(v(evaluation, "MKV.distributionAreaNotes"));
			data.add(pair(evaluation, "MKV.occurrenceAreaMin", "MKV.occurrenceAreaMax"));
			data.add(v(evaluation, "MKV.occurrenceAreaNotes"));
			data.add(v(evaluation, "MKV.occurrenceNotes"));

			for (String areaId : getTaxonomyDAO().getIucnDAO().getEvaluationAreas().keySet()) {
				if (evaluation.hasOccurrence(areaId)) {
					Occurrence o = evaluation.getOccurrence(areaId);
					String label = AREA_STATUSES_FOR_DOWNLOAD.get(o.getStatus());
					if (label == null) label = "UNKNOWN VALUE " + o.getStatus();
					if (Boolean.TRUE.equals(o.getThreatened())) {
						label += " (RT)";
					}
					data.add(label);
				} else {
					data.add("");
				}
			}			
			data.add(v(evaluation, "MKV.occurrenceRegionsNotes"));
			data.add(v(evaluation, "MKV.occurrenceRegionsPrivateNotes"));
			data.add(v(evaluation, "MKV.regionallyThreatenedNotes"));
			data.add(v(evaluation, "MKV.regionallyThreatenedPrivateNotes"));
			data.add(habitat(evaluation.getPrimaryHabitat()));
			data.add(habitats(evaluation.getSecondaryHabitats()));
			data.add(v(evaluation, "MKV.habitatGeneralNotes"));
			data.add(v(evaluation, "MKV.habitatNotes"));
			data.add(d(evaluation, "MKV.generationAge"));
			data.add(v(evaluation, "MKV.generationAgeNotes"));
			data.add(v(evaluation, "MKV.evaluationPeriodLength"));
			data.add(v(evaluation, "MKV.evaluationPeriodLengthNotes"));
			data.add(pair(evaluation, "MKV.individualCountMin", "MKV.individualCountMax"));
			data.add(v(evaluation, "MKV.individualCountNotes"));
			data.add(v(evaluation, "MKV.populationSizePeriodBeginning"));
			data.add(v(evaluation, "MKV.populationSizePeriodEnd"));
			data.add(v(evaluation, "MKV.populationSizePeriodNotes"));
			data.add(v(evaluation, "MKV.decreaseDuringPeriod"));
			data.add(v(evaluation, "MKV.decreaseDuringPeriodNotes"));
			data.add(v(evaluation, "MKV.populationVaries"));
			data.add(v(evaluation, "MKV.populationVariesNotes"));
			data.add(v(evaluation, "MKV.fragmentedHabitats"));
			data.add(v(evaluation, "MKV.fragmentedHabitatsNotes"));
			data.add(v(evaluation, "MKV.borderGain"));
			data.add(v(evaluation, "MKV.borderGainNotes"));

			data.add(endangerment(evaluation.getEndangermentReasons()));
			data.add(v(evaluation, "MKV.endangermentReasonNotes"));
			data.add(endangerment(evaluation.getThreats()));
			data.add(v(evaluation, "MKV.threatNotes"));
			data.add(v(evaluation, "MKV.groundsForEvaluationNotes"));

			for (String criteria : CRITERIAS) {
				data.add(v(evaluation, "MKV.criteria"+criteria));
				data.add(v(evaluation, "MKV.criteria"+criteria+"Notes"));
				data.add(status(evaluation.getValue("MKV.status"+criteria)));
				data.add(v(evaluation, "MKV.status"+criteria+"Notes"));
			}
			data.add(v(evaluation, "MKV.criteriaNotes"));

			data.add(statusWithSymbols(evaluation));
			data.add(v(evaluation, "MKV.criteriaForStatus"));
			data.add(pair(status(evaluation.getValue("MKV.redListStatusMin")), status(evaluation.getValue("MKV.redListStatusMax"))));
			data.add(evaluation.getExternalImpact());
			data.add(reasonForStatusChange(evaluation));
			data.add(enumValue(evaluation, "MKV.ddReason"));
			data.add(status(evaluation.getValue("MKV.possiblyRE")));
			data.add(v(evaluation, "MKV.lastSightingNotes"));
			data.add(v(evaluation, "MKV.lsaRecommendation"));

			data.add(v(evaluation, "MKV.redListStatusAccuracyNotes"));
			data.add(v(evaluation, "MKV.redListStatusNotes"));
			data.add(v(evaluation, "MKV.criteriaForStatusNotes"));
			data.add(v(evaluation, "MKV.externalPopulationImpactOnRedListStatusNotes"));
			data.add(v(evaluation, "MKV.reasonForStatusChangeNotes"));
			data.add(v(evaluation, "MKV.ddReasonNotes"));
			data.add(v(evaluation, "MKV.possiblyRENotes"));
			data.add(v(evaluation, "MKV.lsaRecommendationNotes"));

			data.add(v(evaluation, "MKV.percentageOfGlobalPopulation"));
			data.add(v(evaluation, "MKV.percentageOfGlobalPopulationNotes"));

			data.add(publications(evaluation));
			data.add(v(evaluation, "MKV.otherSources"));

			data.add(" -> ");

			appendRLIValues(target, years, selectedYear, data);
			return data;
		}

		private String groupName(RedListEvaluationGroup group) {
			if (group == null) return MISSING_VALUE;
			return group.getName("fi");
		}

		private RedListEvaluationGroup getSubGroup(RedListEvaluationGroup of, List<RedListEvaluationGroup> in) {
			for (Qname subGroupId : of.getSubGroups()) {
				RedListEvaluationGroup g = getGroup(subGroupId, in); 
				if (g != null) return g;
			}
			return null;
		}

		private RedListEvaluationGroup getGroup(Qname subGroupId, List<RedListEvaluationGroup> in) {
			for (RedListEvaluationGroup g : in) {
				if (g.getQname().equals(subGroupId)) return g;
			}
			return null;
		}

		private RedListEvaluationGroup getGroupRoot(EvaluationTarget target) {
			for (RedListEvaluationGroup group : getGroups(target)) {
				if (group.isRoot()) return group;
			}
			return null;
		}

		private List<RedListEvaluationGroup> getGroups(EvaluationTarget target) {
			Set<Qname> groupIds = target.getTaxon().getRedListEvaluationGroups();
			if (groupIds.isEmpty()) return Collections.emptyList();
			List<RedListEvaluationGroup> groups = new ArrayList<>();
			for (Qname groupId : groupIds) {
				try {
					RedListEvaluationGroup g = getGroup(groupId);
					groups.add(g);
					addParentGroups(groups, g);
				} catch (Exception e) {
					getErrorReporter().report("Missing group / load error for taxon id " + target.getTaxon().getQname() + " groupid " + groupId, e);
				}
			}
			return groups;
		}

		private void addParentGroups(List<RedListEvaluationGroup> groups, RedListEvaluationGroup group) throws Exception {
			for (Qname parentId : group.getParents()) {
				RedListEvaluationGroup parent = getGroup(parentId);
				if (!groups.contains(parent)) {
					groups.add(parent);
					addParentGroups(groups, parent);
				}
			}
		}

		private RedListEvaluationGroup getGroup(Qname groupId) throws Exception {
			RedListEvaluationGroup group = getTaxonomyDAO().getRedListEvaluationGroups().get(groupId.toString());
			if (group == null) throw new IllegalStateException("Missing group " + groupId);
			return group;
		}

		public static void appendRLIValues(EvaluationTarget target, List<Integer> years, int selectedYear, List<String> data) {
			for (Integer year : years) {
				Evaluation yearEval = target.getEvaluation(year);
				if (yearEval == null) {
					data.add(MISSING_VALUE);
					data.add(MISSING_VALUE);
					data.add(MISSING_VALUE);
					data.add(MISSING_VALUE);
				} else {
					data.add(statusWithSymbols(yearEval));
					if (yearEval.hasCorrectedStatusForRedListIndex()) {
						data.add(status(yearEval.getCorrectedStatusForRedListIndex()));
						Integer rliByStatus = yearEval.getCalculatedRedListIndex();
						Integer rliCorrected = yearEval.getCalculatedCorrectedRedListIndex(); 
						if (rliByStatus == null || rliCorrected == null) {
							data.add("Ei voi laskea");
						} else {
							data.add(s(rliByStatus - rliCorrected));
						}
						data.add(s(rliCorrected));	
					} else {
						data.add(status(yearEval.getIucnStatus()));
						data.add("0");
						data.add(s(yearEval.getCalculatedRedListIndex()));
					}
				}
			}
			for (Integer year : years) {
				if (year >= selectedYear) continue;
				Evaluation yearEval = target.getEvaluation(year);
				if (yearEval == null) {
					data.add(MISSING_VALUE);
				} else {
					data.add(v(yearEval, "MKV.redListIndexCorrectionNotes"));
				}
			}
		}

		private static final Map<String, Integer> STATUS_CHANGE_NUMBER_CODES;
		static {
			STATUS_CHANGE_NUMBER_CODES = new HashMap<>();
			STATUS_CHANGE_NUMBER_CODES.put("MKV.reasonForStatusChangeGenuine", 1);
			STATUS_CHANGE_NUMBER_CODES.put("MKV.reasonForStatusChangeGenuineBeforePreviousEvaluation", 2);
			STATUS_CHANGE_NUMBER_CODES.put("MKV.reasonForStatusChangeChangesInCriteria", 3);
			STATUS_CHANGE_NUMBER_CODES.put("MKV.reasonForStatusChangeMoreInformation", 4);
			STATUS_CHANGE_NUMBER_CODES.put("MKV.reasonForStatusChangeChangesInTaxonomy", 5);
			STATUS_CHANGE_NUMBER_CODES.put("MKV.reasonForStatusChangeError", 6);
			STATUS_CHANGE_NUMBER_CODES.put("MKV.reasonForStatusChangeErroneousInformation", 7);
			STATUS_CHANGE_NUMBER_CODES.put("MKV.reasonForStatusChangeOther", 8);
		}

		private String reasonForStatusChange(Evaluation evaluation) {
			if (!evaluation.hasValue("MKV.reasonForStatusChange")) return null;
			List<String> values = new ArrayList<>();
			for (String value : evaluation.getValues("MKV.reasonForStatusChange")) {
				Integer code = STATUS_CHANGE_NUMBER_CODES.get(value);
				if (code == null) {
					values.add(value);
				} else {
					values.add(code.toString());
				}
			}
			return catenade(values);
		}

		private String pair(Evaluation evaluation, String minPredicate, String maxPredicate) {
			String min = evaluation.getValue(minPredicate);
			String max = evaluation.getValue(maxPredicate);
			return pair(min, max);
		}

		private String pair(String s1, String s2) {
			if (!given(s1) && !given(s2)) return null;
			if (!given(s1)) s1 = "";
			if (!given(s2)) s2 = "";
			return s1 + " -- " + s2;
		}

		private static String s(Integer i) {
			if (i == null) return null;
			return i.toString();
		}

		private String publications(Evaluation evaluation) throws Exception {
			List<String> values = evaluation.getValues("MKV.publication");
			if (values.isEmpty()) return null;
			List<String> citations = new ArrayList<>();
			for (String value : values) {
				citations.add(getTaxonomyDAO().getPublications().get(value).getCitation());
			}
			return catenade(citations);
		}

		private String catenade(List<String> values) {
			if (values.isEmpty()) return "";
			StringBuilder b = new StringBuilder();
			Iterator<String> i = values.iterator();
			while (i.hasNext()) {
				b.append(i.next());
				if (i.hasNext()) b.append(", ");
			}
			return b.toString();
		}

		private static String statusWithSymbols(Evaluation evaluation) {
			String status = status(evaluation.getIucnStatus());
			String externalImpact = externalImpact(evaluation);
			String possiblyRE = possiblyRE(evaluation);
			if (given(externalImpact)) {
				status += externalImpact;
			}
			if (given(possiblyRE)) {
				status += possiblyRE;
			}
			return status;
		}

		private static String possiblyRE(Evaluation evaluation) {
			if (evaluation.hasValue("MKV.possiblyRE")) return "●";
			return null;
		}

		private static String externalImpact(Evaluation evaluation) {
			String extImp = evaluation.getExternalImpact();
			if (!given(extImp)) return null;
			if (extImp.equals("-2")) return "°°";
			if (extImp.equals("-1")) return "°";
			if (extImp.equals("+1")) return "✝";
			if (extImp.equals("+2")) return "✝✝";
			return null;
		}

		private String endangerment(List<EndangermentObject> reasons) {
			if (reasons.isEmpty()) return null;
			StringBuilder b = new StringBuilder();
			Iterator<EndangermentObject> i = reasons.iterator();
			while (i.hasNext()) {
				String value = i.next().getEndangerment().toString();
				if (value.equals("MKV.endangermentReasonT")) {
					value = "?";
				} else {
					value = value.replace("MKV.endangermentReason", ""); 
				}
				b.append(value);
				if (i.hasNext()) b.append(", ");
			}
			return b.toString();
		}

		private String d(Evaluation evaluation, String predicate) {
			String s = evaluation.getValue(predicate);
			if (!given(s)) return null;
			return s.replace(".", ",");
		}

		private String habitats(List<HabitatObject> secondaryHabitats) {
			if (secondaryHabitats.isEmpty()) return null;
			List<String> values = new ArrayList<>();
			for (HabitatObject o : secondaryHabitats) {
				String value = habitat(o);
				if (value != null) values.add(value);
			}
			return catenade(values);
		}

		private String habitat(HabitatObject habitat) {
			if (habitat == null) return null;
			StringBuilder b = new StringBuilder();
			if (given(habitat.getHabitat())) {
				b.append(habitat.getHabitat().toString().replace("MKV.habitat", ""));
			}
			b.append(" ");
			for (Qname type : habitat.getHabitatSpecificTypes()) {
				if (given(type)) {
					String s = type.toString().replace("MKV.habitatSpecificType", "").toLowerCase();
					if (s.equals("pak")) s = "pa";
					if (s.equals("vak")) s = "va";
					b.append(s).append(" ");
				}
			}
			return b.toString().trim();
		}

		private static String v(Evaluation evaluation, String predicate) {
			String value = evaluation.getValue(predicate);
			if (value == null) return "";
			if (value.equals("false")) return "Ei";
			if (value.equals("true")) return "Kyllä";
			return value;
		}

		private String enumValue(Evaluation evaluation, String predicate) throws Exception {
			String value = evaluation.getValue(predicate);
			return enumValue(predicate, value);
		}

		private String enumValue(String predicate, String value) throws Exception {
			if (!given(value)) return "";
			return getIUCNProperties().getProperty(predicate).getRange().getValueFor(value).getLabel().forLocale("fi");
		}

		private RdfProperties getIUCNProperties() throws Exception {
			return getTaxonomyDAO().getIucnDAO().getEvaluationProperties();
		}

		private String enumValue(Evaluation evaluation, String predicate, Collection<RdfProperty> range) {
			String value = evaluation.getValue(predicate);
			if (!given(value)) return "";
			for (RdfProperty rangeValue : range) {
				if (rangeValue.getQname().toString().equals(value)) {
					return rangeValue.getLabel().forLocale("fi");
				}
			}
			return "UNKNOWN VALUE";
		}

		private static String status(String iucnStatus) {
			if (iucnStatus == null) return "";
			return iucnStatus.replace("MX.iucn", "");
		}

		private String lastModifiedBy(Evaluation evaluation) throws Exception {
			if (!given(evaluation.getLastModifiedBy())) return MISSING_VALUE;
			Person person = getTaxonomyDAO().getPersons().get(evaluation.getLastModifiedBy());
			if (person == null) return MISSING_VALUE;
			return person.getFullname();
		}

		private String lastModified(Evaluation evaluation) throws Exception {
			if (evaluation.getLastModified() == null) return MISSING_VALUE;
			return DateUtils.format(evaluation.getLastModified(), "d.M.yyyy");
		}

		private String state(Evaluation evaluation) {
			if (evaluation.isReady()) return "Valmis";
			if (evaluation.isReadyForComments()) return "Valmis kommentoitavaksi";
			if (evaluation.getState() == null) return "Aloittamatta";
			return "Kesken";
		}

		private String adminStatuses(EvaluationTarget target) throws Exception {
			RdfProperty property = getTriplestoreDAO().getProperties("MX.taxon").getProperty("MX.hasAdminStatus"); 
			Iterator<Qname> i = target.getTaxon().getAdministrativeStatuses().iterator();
			StringBuilder b = new StringBuilder();
			while (i.hasNext()) {
				Qname status = i.next();
				String label = property.getRange().getValueFor(status.toString()).getLabel().forLocale("fi");
				b.append(label);
				if (i.hasNext()) b.append(", ");
			}
			return b.toString();
		}

		private String taxonRank(EvaluationTarget target) {
			Qname rank = target.getTaxon().getTaxonRank(); 
			if (!given(rank)) {
				return "";
			}
			return rank.toString().replace("MX.", "");
		}

		private List<String> fileDownloadHeaderRow(int selectedYear, List<Integer> years) throws Exception {
			List<String> header = new ArrayList<>();
			header.add("Ryhmä 1");
			header.add("Ryhmä 2");
			header.add("Ryhmä 3");
			header.add("Id");
			header.add("Lahko, Heimo");
			header.add("Taksonominen taso");
			header.add("Tieteellinen nimi");
			header.add("Auktori");
			header.add("Synonyymit");
			header.add("Suomenkielinen nimi");
			header.add("Ruotsinkielinen nimi");
			header.add("Englanninkielinen nimi");
			header.add("Taksonomiatietokannan kommentit");
			header.add("Hallinnollinen asema");
			header.add("Arvioinnin tila");
			header.add("Muokattu");
			header.add("Muokkaaja");
			header.add("Luokka");
			header.add("Edellinen luokka");
			header.add("Edell. RLI korjaus");
			header.add("Kommentit arviosta");
			header.add("ARVIOINNIN TIEDOT ALKAVAT");
			header.add("Arvioinnin kommentit taksonomiasta (julkinen)");
			header.add("Vakinaisuus");
			header.add("..muistiinpanot (yksityinen)");
			header.add("Levinneisuusalueen koko");
			header.add("..muistiinpanot (yksityinen)");
			header.add("Esiinymisalueen koko");
			header.add("..muistiinpanot");
			header.add("Kommentit esiintymisestä (julkinen)");
			for (Area area : getTaxonomyDAO().getIucnDAO().getEvaluationAreas().values()) {
				header.add(area.getName().forLocale("fi"));
			}
			header.add("Kommentit esiintymisalueista (julkinen)");
			header.add("Muistiinpanot esiintymisalueista (yksityinen)");
			header.add("Kommentit alueellisesta uhanalaisuudesta (julkinen)");
			header.add("Muistiinpanot alueellisesta uhanalaisuudesta (yksityinen)");
			header.add("Ensisijainen elinympäristö");
			header.add("Muut elinympäristöt");
			header.add("Kommentit elinympäristöstä (julkinen)");
			header.add("Muistiinpanot elinympäristöistä (yksityinen)");
			header.add("Sukupolvi");
			header.add("..muistiinpanot (yksityinen)");
			header.add("Tarkastelujakson pituus");
			header.add("..muistiinpanot (yksityinen)");
			header.add("Yksilömäärä");
			header.add("..muistiinpanot (yksityinen)");
			header.add("Pop.koko alussa");
			header.add("Pop.koko lopussa");
			header.add("..muistiinpanot (yksityinen)");
			header.add("Pop. väheneminen");
			header.add("..muistiinpanot (yksityinen)");
			header.add("Kannanvaihtelut");
			header.add("..muistiinpanot (yksityinen)");
			header.add("Pop. pirstoutunut");
			header.add("..muistiinpanot (yksityinen)");
			header.add("Rajantakainen vahvistus");
			header.add("..muistiinpanot (yksityinen)");
			header.add("Uhanalaisuuden syyt");
			header.add("..muistiinpanot (yksityinen)");
			header.add("Uhkatekijät");
			header.add("..muistiinpanot (yksityinen)");
			header.add("Kommentit arvioinnin perusteista (julkinen)");
			for (String criteria : CRITERIAS) {
				header.add(criteria + " kriteerit");
				header.add(criteria + "..muistiinpanot (yksityinen)");
				header.add(criteria + " luokka");
				header.add(criteria + "..muistiinpanot (yksityinen)");
			}
			header.add("Kommentit kriteereistä (julkinen)");
			header.add("Luokka");
			header.add("Kriteerit");
			header.add("Vaihteluväli");
			header.add("Alentaminen/ korottaminen");
			header.add("Muutoksen syy");
			header.add("DD-syy");
			header.add("Mahd. hävinnyt");
			header.add("Viimeisin havainto (julkinen)");
			header.add("LSA ehd.");
			header.add("Kommentit arvioinnin tarkkuudesta/luotettavuudesta (julkinen)");
			header.add("Luokka muistiinpanot (yksityinen)");
			header.add("Kriteerit muistiinpanot (yksityinen)");
			header.add("Alentaminen/ korottaminen muistiinpanot (yksityinen)");
			header.add("Muutoksen syy muistiinpanot (yksityinen)");
			header.add("DD-syy muistiinpanot (yksityinen)");
			header.add("Mahd. hävinnyt muistiinpanot (yksityinen)");
			header.add("LSA ehd. muistiinpanot (yksityinen)");
			header.add("Osuus glob.pop.");
			header.add("..muistiinpanot (yksityinen)");
			header.add("Julkaisut");
			header.add("Muut lähteet");
			header.add("RLI TIEDOT ALKAVAT");
			appendRLIHeader(selectedYear, years, header);
			return header;
		}

		public static void appendRLIHeader(int selectedYear, List<Integer> years, List<String> header) {
			for (Integer year : years) {
				header.add(year  + " luokka");
				header.add(year  + " indeksikorjattu luokka");
				header.add(year + " muutos");
				header.add(year + " RLI");
			}
			for (Integer year : years) {
				if (year >= selectedYear) continue;
				header.add(year + " RLI muistiinpanot");
			}
		}

		private static final Map<Qname, String> AREA_STATUSES_FOR_DOWNLOAD;
		static {
			AREA_STATUSES_FOR_DOWNLOAD = new HashMap<>();
			AREA_STATUSES_FOR_DOWNLOAD.put(new Qname("MX.typeOfOccurrenceOccurs"), "x");
			AREA_STATUSES_FOR_DOWNLOAD.put(new Qname("MX.typeOfOccurrenceExtirpated"), "RE");
			AREA_STATUSES_FOR_DOWNLOAD.put(new Qname("MX.typeOfOccurrenceAnthropogenic"), "NA");
			AREA_STATUSES_FOR_DOWNLOAD.put(new Qname("MX.typeOfOccurrenceUncertain"), "p");
			AREA_STATUSES_FOR_DOWNLOAD.put(new Qname("MX.doesNotOccur"), MISSING_VALUE);
		}

		private boolean isFileDownload(HttpServletRequest req) {
			return req.getParameter("download") != null;
		}

		private static class TaxonLoadException extends Exception {
			private static final long serialVersionUID = -6749180766121111373L;
		}

		private List<EvaluationTarget> filter(List<EvaluationTarget> targets, String[] states, String taxon, String[] redListStatuses, String[] prevRedListStatuses, int selectedYear) throws Exception {
			List<EvaluationTarget> filtered = new ArrayList<>();
			if (!given(states) && !given(taxon) && !given(redListStatuses) && !given(prevRedListStatuses)) return targets;

			Set<String> taxonQnames = Collections.emptySet();
			if (given(taxon)) {
				try {
					taxonQnames = getTaxa(taxon);
				} catch (Exception e) {
					throw new TaxonLoadException();
				}
			}

			for (EvaluationTarget target : targets) {
				Evaluation evaluation = target.getEvaluation(selectedYear);
				Evaluation prevEvaluation = target.getPreviousEvaluation(selectedYear);
				if (given(redListStatuses)) {
					if (!redListStatusesMatch(redListStatuses, evaluation)) continue;
				}
				if (given(prevRedListStatuses)) {
					if (!redListStatusesMatch(prevRedListStatuses, prevEvaluation)) continue;
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

		private boolean statesMatch(String[] states, Evaluation evaluation) {
			for (String state : states) {
				if (!given(state)) continue;
				if (state.equals("ready")) {
					if (evaluation != null && evaluation.isReady()) return true;
				} else if (state.equals("readyForComments")) {
					if (evaluation != null && evaluation.isReadyForComments()) return true;
				} else if (state.equals("started")) {
					if (evaluation != null && evaluation.isStarted()) return true;
				} else if (state.equals("notStarted")) {
					if (evaluation == null) return true;
				} else {
					throw new UnsupportedOperationException(state);
				}
			}
			return false;
		}

		private boolean redListStatusesMatch(String[] redListStatuses, Evaluation evaluation) {
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

		private Set<String> getTaxa(String taxon) throws Exception {
			TaxonSearchResponse result = getTaxonomyDAO().search(new TaxonSearch(taxon, 1000).onlyExact());
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

		private List<EvaluationTarget> pageTargets(int currentPage, int pageSize, List<EvaluationTarget> targets) {
			if (targets.isEmpty()) return targets;
			List<EvaluationTarget> list = new ArrayList<>();
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
				if (i < 1) return 10;
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
