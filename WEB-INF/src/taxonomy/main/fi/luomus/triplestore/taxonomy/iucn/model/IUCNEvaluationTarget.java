package fi.luomus.triplestore.taxonomy.iucn.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IUCNEvaluationTarget {

	private final String qname;
	private final String scientificName;
	private final String vernacularNameFi;
	private final Map<Integer, IUCNEvaluation> evaluations = new HashMap<>();
	private final IUCNContainer container;

	public IUCNEvaluationTarget(String qname, String scientificName, String vernacularNameFi, IUCNContainer container) {
		this.qname = qname;
		this.scientificName = scientificName;
		this.vernacularNameFi = vernacularNameFi;
		this.container = container;
	}

	public List<String> getGroups() {
		return container.getGroupsOfTarget(qname);
	}

	public String getQname() {
		return qname;
	}

	public String getScientificName() {
		return scientificName;
	}

	public String getVernacularNameFi() {
		return vernacularNameFi;
	}

	public void setEvaluation(IUCNEvaluation evaluation) {
		evaluations.put(evaluation.getEvaluationYear(), evaluation);
	}

	public IUCNEvaluation getEvaluation(int year) {
		return evaluations.get(year);
	}

	public boolean hasEvaluation(int year) {
		return getEvaluation(year) != null;
	}

	public IUCNEvaluation getPreviousEvaluation(int year) {
		Integer prevYear = getPreviousYear(year);
		if (prevYear == null) return null;
		return getEvaluation(prevYear);
	}

	private Integer getPreviousYear(int year) {
		Integer prevYear = null;
		for (Integer y : getYears()) {
			if (y.equals(year)) {
				return prevYear;
			} else {
				prevYear = y;
			}
		}
		return null;
	}

	private List<Integer> getYears() {
		ArrayList<Integer> years = new ArrayList<>(evaluations.keySet());
		Collections.sort(years);
		return years;
	}

	public boolean hasPreviousEvaluation(int year) {
		return getPreviousEvaluation(year) != null;
	}

}
