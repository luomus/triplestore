package fi.luomus.triplestore.taxonomy.models;

import fi.luomus.commons.containers.rdf.Qname;

import java.util.Collection;

public class TaxonGroupIucnEvaluationData {

	private final Qname groupQname;
	private final Collection<String> speciesOfGroup;
	
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
		return new EvaluationYear(year);
	}
	
	public static class EvaluationYear {
		private final int year;
		public EvaluationYear(int year) {
			this.year = year;
		}
		public int getReadyCount() {
			return 480;
		}
	}
	
}

