package fi.luomus.triplestore.taxonomy.iucn.model;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.RdfProperties;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.taxonomy.Occurrences.Occurrence;
import fi.luomus.commons.utils.DateUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IUCNEvaluation {

	public static final String STATE = "MKV.state";
	public static final String STATE_READY = "MKV.stateReady";
	public static final String RED_LIST_STATUS = "MKV.redListStatus";
	public static final String LAST_MODIFIED_BY = "MKV.lastModifiedBy";
	public static final String LAST_MODIFIED = "MKV.lastModified";
	public static final String EVALUATION_YEAR = "MKV.evaluationYear";
	public static final String EVALUATED_TAXON = "MKV.evaluatedTaxon";
	public static final String RED_LIST_INDEX_CORRECTION = "MKV.redListIndexCorrection";
	public static final String NE_MARK_NOTES = "Merkitty ei-arvioitavaksi pikatoiminnolla.";
	public static final String RED_LIST_STATUS_NOTES = "MKV.redListStatusNotes";

	public static final Map<String, Integer> RED_LIST_STATUS_TO_INDEX;
	static {
		RED_LIST_STATUS_TO_INDEX = new HashMap<>(); // TODO get real mapping from Aino
		RED_LIST_STATUS_TO_INDEX.put("MX.iucnEX", 5);
		RED_LIST_STATUS_TO_INDEX.put("MX.iucnEW", 5);
		RED_LIST_STATUS_TO_INDEX.put("MX.iucnRE", 5);
		RED_LIST_STATUS_TO_INDEX.put("MX.iucnCR", 4);
		RED_LIST_STATUS_TO_INDEX.put("MX.iucnEN", 3);
		RED_LIST_STATUS_TO_INDEX.put("MX.iucnVU", 2);
		RED_LIST_STATUS_TO_INDEX.put("MX.iucnNT", 1);
		RED_LIST_STATUS_TO_INDEX.put("MX.iucnLC", 0);
		RED_LIST_STATUS_TO_INDEX.put("MX.iucnDD", 0);
		RED_LIST_STATUS_TO_INDEX.put("MX.iucnNA", 0);
		RED_LIST_STATUS_TO_INDEX.put("MX.iucnNE", 0); 
	}

	private final RdfProperties evaluationProperties;
	private final Model evaluation;
	private Map<String, Occurrence> occurrences = null;

	public IUCNEvaluation(Model evaluation, RdfProperties evaluationProperties) {
		this.evaluation = evaluation;
		this.evaluationProperties = evaluationProperties;
	}

	public void addOccurrence(Occurrence occurrence) {
		if (occurrence == null) occurrences = new HashMap<>();
		occurrences.put(occurrence.getArea().toString(), occurrence);
	}

	public boolean hasOccurrence(String areaQname) {
		if (occurrences == null) return false;
		return occurrences.containsKey(areaQname);
	}

	public Occurrence getOccurrence(String areaQname) {
		if (occurrences == null) return null;
		return occurrences.get(areaQname);
	}

	public String getId() {
		return evaluation.getSubject().getQname();
	}

	public boolean hasValue(String predicateQname) {
		validate(predicateQname);
		return evaluation.hasStatements(predicateQname);
	}

	public String getValue(String predicateQname) {
		validate(predicateQname);
		if (!evaluation.hasStatements(predicateQname)) return null;
		Statement s = evaluation.getStatements(predicateQname).get(0); 
		if (s.isLiteralStatement()) return s.getObjectLiteral().getContent();
		return s.getObjectResource().getQname();
	}

	private void validate(String predicateQname) {
		if (!evaluationProperties.hasProperty(predicateQname)) throw new IllegalArgumentException("No such property: " + predicateQname);
	}

	public List<String> getValues(String predicateQname) {
		validate(predicateQname);
		if (!evaluation.hasStatements(predicateQname)) return Collections.emptyList();
		List<String> values = new ArrayList<>();
		for (Statement s : evaluation.getStatements(predicateQname)) {
			if (s.isLiteralStatement()) {
				values.add(s.getObjectLiteral().getContent()); 
			} else {
				values.add(s.getObjectResource().getQname());
			}
		}
		return values;
	}

	public Integer getEvaluationYear() {
		if (hasValue(EVALUATION_YEAR)) {
			return Integer.valueOf(getValue(EVALUATION_YEAR));
		}
		return null;
	}

	public boolean isReady() {
		return STATE_READY.equals(getState());
	}

	private String getState() {
		return getValue(STATE);
	}

	public Date getLastModified() throws Exception {
		if (evaluation.hasStatements(LAST_MODIFIED)) {
			return DateUtils.convertToDate(getValue(LAST_MODIFIED), "yyyy-MM-dd");
		}
		return null;
	}

	public String getLastModifiedBy() {
		return getValue(LAST_MODIFIED_BY);
	}

	public String getIucnStatus() {
		return getValue(RED_LIST_STATUS);
	}

	public boolean hasIucnStatus() {
		return evaluation.hasStatements(RED_LIST_STATUS);
	}

	public boolean hasCorrectedIndex() {
		return evaluation.hasStatements(RED_LIST_INDEX_CORRECTION);
	}

	public Integer getCorrectedIucnIndex() {
		if (hasCorrectedIndex()) {
			return Integer.valueOf(getValue(RED_LIST_INDEX_CORRECTION));
		}
		return null;
	}

	public Integer getCalculatedIucnIndex() {
		if (!hasIucnStatus()) return null;
		String iucnStatus = getIucnStatus();
		if (!RED_LIST_STATUS_TO_INDEX.containsKey(iucnStatus)) throw new UnsupportedOperationException("Unknown redListStatus " + iucnStatus);
		return RED_LIST_STATUS_TO_INDEX.get(iucnStatus);
	}

	public String getSpeciesQname() {
		return getValue(EVALUATED_TAXON);
	}

	public Model getModel() {
		return evaluation;
	}

}
