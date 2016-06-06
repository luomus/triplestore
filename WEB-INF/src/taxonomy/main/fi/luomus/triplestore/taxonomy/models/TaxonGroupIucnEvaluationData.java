package fi.luomus.triplestore.taxonomy.models;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.ObjectLiteral;
import fi.luomus.commons.containers.rdf.ObjectResource;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.utils.DateUtils;
import fi.luomus.triplestore.dao.TriplestoreDAO;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TaxonGroupIucnEvaluationData {

	private static final String RED_LIST_INDEX_CORRECTION = "MKV.redListIndexCorrection";
	private static final String STATE_READY = "MKV.stateReady";
	private static final String STATE = "MKV.state";
	private static final String LAST_MODIFIED = "MKV.lastModified";
	private static final String LAST_MODIFIED_BY = "MKV.lastModifiedBy";
	private static final String RED_LIST_STATUS = "MKV.redListStatus";

	private static final Map<String, Integer> CLASS_TO_INDEX;
	static {
		CLASS_TO_INDEX = new HashMap<>(); // TODO get real mapping from Aino
		CLASS_TO_INDEX.put("MX.iucnEX", 5);
		CLASS_TO_INDEX.put("MX.iucnEW", 5);
		CLASS_TO_INDEX.put("MX.iucnRE", 5);
		CLASS_TO_INDEX.put("MX.iucnCR", 4);
		CLASS_TO_INDEX.put("MX.iucnEN", 3);
		CLASS_TO_INDEX.put("MX.iucnVU", 2);
		CLASS_TO_INDEX.put("MX.iucnNT", 1);
		CLASS_TO_INDEX.put("MX.iucnLC", 0);
		CLASS_TO_INDEX.put("MX.iucnDD", 0);
		CLASS_TO_INDEX.put("MX.iucnNA", 0);
		CLASS_TO_INDEX.put("MX.iucnNE", 0); 
	}

	private final Qname groupQname;
	private final Map<String, SpeciesInfo> speciesOfGroup = new LinkedHashMap<>();
	private final Map<Integer, EvaluationYearData> years = new HashMap<>();

	public TaxonGroupIucnEvaluationData(Qname groupQname, List<SpeciesInfo> speciesOfGroup) {
		this.groupQname = groupQname;
		for (SpeciesInfo info : speciesOfGroup) {
			this.speciesOfGroup.put(info.getQname(), info);
		}
	}

	public Collection<SpeciesInfo> getSpeciesOfGroup() {
		return speciesOfGroup.values();
	}

	public EvaluationYearData getYear(int year) {
		if (!years.containsKey(year)) {
			years.put(year, new EvaluationYearData(year, this));
		}
		return years.get(year);
	}

	public SpeciesInfo getSpecies(String speciesQname) {
		return speciesOfGroup.get(speciesQname);
	}

	public static class EvaluationYearData {

		private final int year;
		private final TaxonGroupIucnEvaluationData baseData;
		private final Map<String, EvaluationYearSpeciesData> speciesEvaluations = new HashMap<>();
		private Integer readyCount = null;

		public EvaluationYearData(int year, TaxonGroupIucnEvaluationData taxonGroupIucnEvaluationData) {
			this.year = year;
			this.baseData = taxonGroupIucnEvaluationData;
		}

		public Collection<SpeciesInfo> getSpecies() {
			return baseData.getSpeciesOfGroup();
		}

		public int getReadyCount() {
			if (readyCount != null) return readyCount;
			readyCount = countReady();
			return readyCount;
		}

		private int countReady() {
			int count = 0;
			for (EvaluationYearSpeciesData speciesData : speciesEvaluations.values()) {
				if (speciesData.isReady()) {
					count++;
				}
			}
			return count;
		}

		public EvaluationYearData setEvaluation(String speciesQname, Model evaluation) {
			readyCount = null;
			speciesEvaluations.put(speciesQname, new EvaluationYearSpeciesData(baseData.getSpecies(speciesQname), evaluation, this));
			return this;
		}

		public EvaluationYearSpeciesData getEvaluation(String speciesQname) {
			return speciesEvaluations.get(speciesQname);
		}

		public EvaluationYearSpeciesData markNotEvaluated(String speciesQname, Qname qname, TriplestoreDAO dao) throws Exception {
			Model evaluation = new Model(dao.getSeqNextValAndAddResource("MKV"));
			evaluation.setType("MKV.iucnRedListEvaluation");
			evaluation.addStatement(new Statement(new Predicate("MKV.evaluatedTaxon"), new ObjectResource(speciesQname)));
			evaluation.addStatement(new Statement(new Predicate("MKV.evaluationYear"), new ObjectLiteral(String.valueOf(year))));
			evaluation.addStatement(new Statement(new Predicate("MKV.lastModified"), new ObjectLiteral(DateUtils.getCurrentDate())));
			evaluation.addStatement(new Statement(new Predicate("MKV.lastModifiedBy"), new ObjectResource(qname)));
			evaluation.addStatement(new Statement(new Predicate("MKV.redListStatus"), new ObjectResource("MX.iucnNE")));
			evaluation.addStatement(new Statement(new Predicate("MKV.redListStatusNotes"), new ObjectLiteral("Merkitty ei-arvioitavaksi pikatoiminnolla.", "fi")));
			evaluation.addStatement(new Statement(new Predicate("MKV.state"), new ObjectResource("MKV.stateReady")));
			dao.store(evaluation);
			this.setEvaluation(speciesQname, evaluation);
			return getEvaluation(speciesQname);
		}

	}

	public static class EvaluationYearSpeciesData {

		private final SpeciesInfo speciesInfo;
		private final EvaluationYearData yearData;
		private final Model evaluationData;

		public EvaluationYearSpeciesData(SpeciesInfo speciesInfo, Model evaluationData, EvaluationYearData yearData) {
			this.speciesInfo = speciesInfo;
			this.evaluationData = evaluationData;
			this.yearData = yearData;
		}

		public boolean isReady() {
			return STATE_READY.equals(getState());
		}

		private String getState() {
			return evaluationData.getStatements(STATE).get(0).getObjectResource().getQname();
		}

		public Date getLastModified() throws Exception {
			if (evaluationData.hasStatements(LAST_MODIFIED)) {
				return DateUtils.convertToDate(evaluationData.getStatements(LAST_MODIFIED).get(0).getObjectLiteral().getContent(), "yyyy-MM-dd");
			}
			return null;
		}

		public String getLastModifiedBy() {
			if (evaluationData.hasStatements(LAST_MODIFIED_BY)) {
				return evaluationData.getStatements(LAST_MODIFIED_BY).get(0).getObjectResource().getQname();
			}
			return null;
		}

		public String getIucnClass() {
			if (hasIucnClass()) {
				return evaluationData.getStatements(RED_LIST_STATUS).get(0).getObjectResource().getQname();
			}
			return null;
		}

		public boolean hasIucnClass() {
			return evaluationData.hasStatements(RED_LIST_STATUS);
		}

		public boolean hasCorrectedIndex() {
			return evaluationData.hasStatements(RED_LIST_INDEX_CORRECTION);
		}

		public Integer getCorrectedIucnIndex() {
			if (hasCorrectedIndex()) {
				return Integer.valueOf(evaluationData.getStatements(RED_LIST_INDEX_CORRECTION).get(0).getObjectLiteral().getContent());
			}
			return null;
		}

		public Integer getCalculatedIucnIndex() {
			if (!hasIucnClass()) return null;
			String iucnClass = getIucnClass();
			if (!CLASS_TO_INDEX.containsKey(iucnClass)) throw new UnsupportedOperationException("Unknown class " + iucnClass);
			return CLASS_TO_INDEX.get(iucnClass);
		}

		public SpeciesInfo getSpecies() {
			return speciesInfo;
		}

	}

	public static class SpeciesInfo {
		private final String qname;
		private final String scientificName;
		private final String vernacularNameFi;
		public SpeciesInfo(String qname, String scientificName, String vernacularNameFi) {
			this.qname = qname;
			this.scientificName = scientificName;
			this.vernacularNameFi = vernacularNameFi;
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
	}

}

