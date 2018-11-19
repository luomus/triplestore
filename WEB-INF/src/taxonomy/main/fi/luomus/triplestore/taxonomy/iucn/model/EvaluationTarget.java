package fi.luomus.triplestore.taxonomy.iucn.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.taxonomy.TaxonContainer;
import fi.luomus.commons.taxonomy.iucn.Evaluation;

public class EvaluationTarget {

	private static final Qname FAMILY = new Qname("MX.family");
	private static final Qname ORDER = new Qname("MX.order");
	private final Qname taxonId;
	private final Map<Integer, Evaluation> evaluations = new TreeMap<>(new Comparator<Integer>() {
		@Override
		public int compare(Integer o1, Integer o2) {
			return o2.compareTo(o1);
		}
	});
	private final Container container;
	private final TaxonContainer taxonContainer;

	public EvaluationTarget(Qname taxonId, Container container, TaxonContainer taxonContainer) {
		this.taxonId = taxonId;
		this.container = container;
		this.taxonContainer = taxonContainer;
	}

	public List<String> getGroups() {
		return container.getGroupsOfTarget(getQname());
	}

	public String getQname() {
		return taxonId.toString();
	}

	public String getScientificName() {
		String s = getTaxon().getScientificName();
		if (!given(s)) return getQname();
		return s;
	}

	public String getVernacularNameFi() {
		return getTaxon().getVernacularName() == null ? "" : getTaxon().getVernacularName().forLocale("fi"); 
	}

	public Taxon getTaxon() {
		return taxonContainer.getTaxon(taxonId);
	}

	public String getSynonymNames() {
		StringBuilder b = new StringBuilder();
		Iterator<Taxon> i = getTaxon().getAllSynonyms().iterator();
		while (i.hasNext()) {
			Taxon synonym = i.next();
			if (given(synonym.getScientificName())) {
				b.append(synonym.getScientificName());
				if (i.hasNext()) b.append(", ");
			}
		}
		return b.toString();
	}

	public String getVernacularNames() {
		StringBuilder b = new StringBuilder();
		List<String> names = new ArrayList<>();
		names.addAll(getTaxon().getVernacularName().getAllTexts().values());
		names.addAll(getTaxon().getAlternativeVernacularNames().getAllValues());
		Iterator<String> i = names.iterator();
		while (i.hasNext()) {
			b.append(i.next());
			if (i.hasNext()) b.append(", ");
		}
		return b.toString();
	}

	public String getOrderAndFamily() {
		StringBuilder b = new StringBuilder();
		String className = getTaxon().getScientificNameOfRank(ORDER);
		String familyName = getTaxon().getScientificNameOfRank(FAMILY);
		if (given(className)) {
			b.append(className);
			if (given(familyName)) {
				b.append(", ");
			}
		}
		if (given(familyName)) {
			b.append(familyName);
		}
		return b.toString();
	}

	private boolean given(String s) {
		return s != null && s.length() > 0;
	}

	public void setEvaluation(Evaluation evaluation) {
		evaluations.put(evaluation.getEvaluationYear(), evaluation);
	}

	public Evaluation getEvaluation(int year) throws Exception {
		return evaluations.get(year);
	}

	public boolean hasEvaluation(int year) throws Exception {
		return getEvaluation(year) != null;
	}

	public Evaluation getPreviousEvaluation(int year) throws Exception {
		Integer prevYear = getPreviousYear(year);
		if (prevYear == null) return null;
		return getEvaluation(prevYear);
	}

	private Integer getPreviousYear(int year) {
		Integer prevYear = null;
		for (Integer y : getYears()) {
			if (y.intValue() >= year) break;
			if (y.intValue() == year) {
				return prevYear;
			} else {
				prevYear = y;
			}
		}
		return prevYear;
	}

	private List<Integer> getYears() {
		return Collections.unmodifiableList(new ArrayList<>(evaluations.keySet()));
	}

	public boolean hasPreviousEvaluation(int year) throws Exception {
		return getPreviousEvaluation(year) != null;
	}

	public Collection<Evaluation> getEvaluations() {
		return evaluations.values();
	}

	public boolean hasEvaluations() {
		return !evaluations.isEmpty();
	}

	public void removeEvaluation(Evaluation evaluation) {
		evaluations.remove(evaluation.getEvaluationYear());
	}

	@Override
	public String toString() {
		return "IUCNEvaluationTarget [getGroups()=" + getGroups() + ", getYears()=" + getYears() + "]";
	}

	public Evaluation getLatestEvaluation() throws Exception {
		return getPreviousEvaluation(Integer.MAX_VALUE);
	}


}
