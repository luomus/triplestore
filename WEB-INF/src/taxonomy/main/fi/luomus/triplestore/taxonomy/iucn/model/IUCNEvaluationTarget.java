package fi.luomus.triplestore.taxonomy.iucn.model;

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
		evaluations.put(evaluation.getYear(), evaluation);
	}

	public IUCNEvaluation getEvaluation(int year) {
		return evaluations.get(year);
	}

}
