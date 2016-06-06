package fi.luomus.triplestore.taxonomy.models;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.Qname;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TaxonGroupIucnEvaluationData {

	private final Qname groupQname;
	private final Collection<String> speciesOfGroup;
	private final Map<Integer, EvaluationYear> years = new HashMap<>();

	public TaxonGroupIucnEvaluationData(Qname groupQname, Collection<String> speciesOfGroup) {
		this.groupQname = groupQname;
		this.speciesOfGroup = speciesOfGroup;
	}

	public Qname getGroupQname() {
		return groupQname;
	}

	public Collection<String> getSpeciesOfGroup() {
		return speciesOfGroup;
	}

	public EvaluationYear getYear(int year) {
		if (!years.containsKey(years)) {
			years.put(year, new EvaluationYear(year, this));
		}
		return years.get(year);
	}

	public static class EvaluationYear {
		private final int year;
		private final TaxonGroupIucnEvaluationData baseData;
		private final Map<String, Model> speciesEvaluations = new HashMap<>();
		private Integer readyCount = null;

		public EvaluationYear(int year, TaxonGroupIucnEvaluationData taxonGroupIucnEvaluationData) {
			this.year = year;
			this.baseData = taxonGroupIucnEvaluationData;
		}
		public int getReadyCount() {
			if (readyCount != null) return readyCount;
			readyCount = countReady();
			return readyCount;
		}
		private int countReady() {
			int count = 0;
			for (Model evaluation : speciesEvaluations.values()) {
				if (evaluation.getStatements("MKV.state").get(0).getObjectResource().getQname().equals("MKV.stateReady")) {
					count++;
				}
			}
			return count;
		}		
		public EvaluationYear setEvaluation(String speciesQname, Model evaluation) {
			readyCount = null;
			speciesEvaluations.put(speciesQname, evaluation);
			return this;
		}

	}

}

