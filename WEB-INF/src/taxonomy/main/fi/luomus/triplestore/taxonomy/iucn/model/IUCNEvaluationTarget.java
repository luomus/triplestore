package fi.luomus.triplestore.taxonomy.iucn.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.taxonomy.Taxon;

public class IUCNEvaluationTarget {

	private static final Qname FAMILY = new Qname("MX.family");
	private static final Qname ORDER = new Qname("MX.order");
	private final Taxon taxon;
	private final Map<Integer, IUCNEvaluation> evaluations = new HashMap<>();
	private final IUCNContainer container;

	public IUCNEvaluationTarget(Taxon taxon, IUCNContainer container) {
		this.taxon = taxon;
		this.container = container;
	}

	public List<String> getGroups() {
		return container.getGroupsOfTarget(getQname());
	}

	public String getQname() {
		return taxon.getQname().toString();
	}

	public String getScientificName() {
		String s = taxon.getScientificName();
		if (!given(s)) return taxon.getQname().toString();
		return s;
	}

	public String getVernacularNameFi() {
		return taxon.getVernacularName() == null ? "" : taxon.getVernacularName().forLocale("fi"); 
	}

	public Taxon getTaxon() {
		return taxon;
	}

	public String getSynonymNames() {
		StringBuilder b = new StringBuilder();
		Iterator<Taxon> i = taxon.getAllSynonyms().iterator();
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
		names.addAll(taxon.getVernacularName().getAllTexts().values());
		names.addAll(taxon.getAlternativeVernacularNames().getAllValues());
		Iterator<String> i = names.iterator();
		while (i.hasNext()) {
			b.append(i.next());
			if (i.hasNext()) b.append(", ");
		}
		return b.toString();
	}

	public String getOrderAndFamily() {
		StringBuilder b = new StringBuilder();
		String className = taxon.getScientificNameOfRank(ORDER);
		String familyName = taxon.getScientificNameOfRank(FAMILY);
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

	public void setEvaluation(IUCNEvaluation evaluation) {
		evaluations.put(evaluation.getEvaluationYear(), evaluation);
	}

	public IUCNEvaluation getEvaluation(int year) throws Exception {
		return evaluations.get(year);
	}

	public boolean hasEvaluation(int year) throws Exception {
		return getEvaluation(year) != null;
	}

	public IUCNEvaluation getPreviousEvaluation(int year) throws Exception {
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
		ArrayList<Integer> years = new ArrayList<>(evaluations.keySet());
		Collections.sort(years);
		return years;
	}

	public boolean hasPreviousEvaluation(int year) throws Exception {
		return getPreviousEvaluation(year) != null;
	}

	public Collection<IUCNEvaluation> getEvaluations() {
		return evaluations.values();
	}

	public boolean hasEvaluations() {
		return !evaluations.isEmpty();
	}

	public void removeEvaluation(IUCNEvaluation evaluation) {
		evaluations.remove(evaluation.getEvaluationYear());
	}

}
