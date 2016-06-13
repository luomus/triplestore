package fi.luomus.triplestore.taxonomy.iucn.model;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.ObjectLiteral;
import fi.luomus.commons.containers.rdf.ObjectResource;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.utils.DateUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IUCNEvaluation {

	public static final String IUCN_EVALUATION_NAMESPACE = "MKV";
	private static final String STATE = "MKV.state";
	private static final String STATE_READY = "MKV.stateReady";
	private static final String NE_MARK_NOTES = "Merkitty ei-arvioitavaksi pikatoiminnolla.";
	private static final String RED_LIST_STATUS_NOTES = "MKV.redListStatusNotes";
	private static final String RED_LIST_STATUS = "MKV.redListStatus";
	private static final String LAST_MODIFIED_BY = "MKV.lastModifiedBy";
	private static final String LAST_MODIFIED = "MKV.lastModified";
	private static final String EVALUATION_YEAR = "MKV.evaluationYear";
	private static final String EVALUATED_TAXON = "MKV.evaluatedTaxon";
	private static final String IUCN_RED_LIST_EVALUATION_CLASS = "MKV.iucnRedListEvaluation";
	private static final String RED_LIST_INDEX_CORRECTION = "MKV.redListIndexCorrection";

	public static final Map<String, Integer> CLASS_TO_INDEX;
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

	private final Model evaluation;
	private final int year;

	public IUCNEvaluation(Model evaluation) {
		this.evaluation = evaluation;
		this.year = getEvaluationYear();
	}

	public String getId() {
		return evaluation.getSubject().getQname();
	}
	
	public int getYear() {
		return year;
	}

	public String getValue(String predicateQname) {
		if (!evaluation.hasStatements(predicateQname)) return "";
		Statement s = evaluation.getStatements(predicateQname).get(0); 
		if (s.isLiteralStatement()) return s.getObjectLiteral().getContent();
		return s.getObjectResource().getQname();
	}
	
	public List<String> getValues(String predicateQname) {
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
	
	private int getEvaluationYear() {
		return Integer.valueOf(evaluation.getStatements(EVALUATION_YEAR).get(0).getObjectLiteral().getContent());
	}

	public boolean isReady() {
		return STATE_READY.equals(getState());
	}

	private String getState() {
		return evaluation.getStatements(STATE).get(0).getObjectResource().getQname();
	}

	public Date getLastModified() throws Exception {
		if (evaluation.hasStatements(LAST_MODIFIED)) {
			return DateUtils.convertToDate(evaluation.getStatements(LAST_MODIFIED).get(0).getObjectLiteral().getContent(), "yyyy-MM-dd");
		}
		return null;
	}

	public String getLastModifiedBy() {
		if (evaluation.hasStatements(LAST_MODIFIED_BY)) {
			return evaluation.getStatements(LAST_MODIFIED_BY).get(0).getObjectResource().getQname();
		}
		return null;
	}

	public String getIucnStatus() {
		if (hasIucnStatus()) {
			return evaluation.getStatements(RED_LIST_STATUS).get(0).getObjectResource().getQname();
		}
		return null;
	}

	public boolean hasIucnStatus() {
		return evaluation.hasStatements(RED_LIST_STATUS);
	}

	public boolean hasCorrectedIndex() {
		return evaluation.hasStatements(RED_LIST_INDEX_CORRECTION);
	}

	public Integer getCorrectedIucnIndex() {
		if (hasCorrectedIndex()) {
			return Integer.valueOf(evaluation.getStatements(RED_LIST_INDEX_CORRECTION).get(0).getObjectLiteral().getContent());
		}
		return null;
	}

	public Integer getCalculatedIucnIndex() {
		if (!hasIucnStatus()) return null;
		String iucnClass = getIucnStatus();
		if (!CLASS_TO_INDEX.containsKey(iucnClass)) throw new UnsupportedOperationException("Unknown class " + iucnClass);
		return CLASS_TO_INDEX.get(iucnClass);
	}

	public String getSpeciesQname() {
		return evaluation.getStatements(EVALUATED_TAXON).get(0).getObjectResource().getQname();
	}

	public static IUCNEvaluation notEvaluated(Qname evaluationId, String speciesQname, int year, Qname editorQname) {
		Model evaluation = new Model(evaluationId);
		evaluation.setType(IUCN_RED_LIST_EVALUATION_CLASS);
		evaluation.addStatement(new Statement(new Predicate(EVALUATED_TAXON), new ObjectResource(speciesQname)));
		evaluation.addStatement(new Statement(new Predicate(EVALUATION_YEAR), new ObjectLiteral(String.valueOf(year))));
		evaluation.addStatement(new Statement(new Predicate(LAST_MODIFIED), new ObjectLiteral(DateUtils.getCurrentDate())));
		evaluation.addStatement(new Statement(new Predicate(LAST_MODIFIED_BY), new ObjectResource(editorQname)));
		evaluation.addStatement(new Statement(new Predicate(RED_LIST_STATUS), new ObjectResource("MX.iucnNE")));
		evaluation.addStatement(new Statement(new Predicate(RED_LIST_STATUS_NOTES), new ObjectLiteral(NE_MARK_NOTES, "fi")));
		evaluation.addStatement(new Statement(new Predicate(STATE), new ObjectResource(STATE_READY)));
		return new IUCNEvaluation(evaluation);
	}

	public Model getModel() {
		return evaluation;
	}

}
