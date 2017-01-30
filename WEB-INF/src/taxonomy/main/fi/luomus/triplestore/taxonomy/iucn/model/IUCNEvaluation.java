package fi.luomus.triplestore.taxonomy.iucn.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.ObjectLiteral;
import fi.luomus.commons.containers.rdf.ObjectResource;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.RdfProperties;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.taxonomy.Occurrences.Occurrence;
import fi.luomus.commons.utils.DateUtils;
import fi.luomus.commons.utils.Utils;

public class IUCNEvaluation {

	public static final String TAXONOMIC_NOTES = "MKV.taxonomicNotes";
	public static final String POSSIBLY_RE = "MKV.possiblyRE";
	public static final String POPULATION_SIZE_PERIOD_BEGINNING = "MKV.populationSizePeriodBeginning"; 
	public static final String POPULATION_SIZE_PERIOD_END = "MKV.populationSizePeriodEnd";
	public static final String POPULATION_SIZE_PERIOD_NOTES = "MKV.populationSizePeriodNotes";
	public static final String OCCURRENCE_AREA_NOTES = "MKV.occurrenceAreaNotes";
	public static final String LEGACY_PUBLICATIONS = "MKV.legacyPublications";
	public static final String INDIVIDUAL_COUNT_NOTES = "MKV.individualCountNotes";
	public static final String GROUNDS_FOR_EVALUATION_NOTES = "MKV.groundsForEvaluationNotes";
	public static final String DISTRIBUATION_AREA_NOTES = "MKV.distributionAreaNotes";
	public static final String DECREASE_DURING_PERIOD = "MKV.decreaseDuringPeriod";
	public static final String REASON_FOR_STATUS_CHANGE = "MKV.reasonForStatusChange";
	public static final String LAST_SIGHTING_NOTES = "MKV.lastSightingNotes";
	public static final String BORDER_GAIN = "MKV.borderGain";
	public static final String FRAGMENTED_HABITATS = "MKV.fragmentedHabitats";
	public static final String POPULATION_VARIES = "MKV.populationVaries";
	public static final String EVALUATION_PERIOD_LENGTH = "MKV.evaluationPeriodLength";
	public static final String GENERATION_AGE = "MKV.generationAge";
	public static final String HABITAT_GENERAL_NOTES = "MKV.habitatGeneralNotes";
	public static final String OCCURRENCE_REGIONS_NOTES = "MKV.occurrenceRegionsNotes";
	public static final String OCCURRENCE_NOTES = "MKV.occurrenceNotes";
	public static final String OCCURRENCE_AREA_MAX = "MKV.occurrenceAreaMax";
	public static final String OCCURRENCE_AREA_MIN = "MKV.occurrenceAreaMin";
	public static final String DISTRIBUTION_AREA_MAX = "MKV.distributionAreaMax";
	public static final String DISTRIBUTION_AREA_MIN = "MKV.distributionAreaMin";
	public static final String TYPE_OF_OCCURRENCE_IN_FINLAND = "MKV.typeOfOccurrenceInFinland";
	public static final String IUCN_EVALUATION_NAMESPACE = "MKV";
	public static final String IUCN_RED_LIST_EVALUATION_YEAR_CLASS = "MKV.iucnRedListEvaluationYear";
	public static final String EVALUATION_CLASS = "MKV.iucnRedListEvaluation";
	public static final String EVALUATION_YEAR = "MKV.evaluationYear";
	public static final String EVALUATED_TAXON = "MKV.evaluatedTaxon";
	public static final String STATE = "MKV.state";
	public static final String STATE_READY = "MKV.stateReady";
	public static final String STATE_STARTED = "MKV.stateStarted";
	public static final String RED_LIST_STATUS = "MKV.redListStatus";
	public static final String HABITAT_OBJECT_CLASS = "MKV.habitatObject";
	public static final String PRIMARY_HABITAT = "MKV.primaryHabitat";
	public static final String SECONDARY_HABITAT = "MKV.secondaryHabitat";
	public static final String HABITAT = "MKV.habitat";
	public static final String HABITAT_SPECIFIC_TYPE = "MKV.habitatSpecificType";
	public static final String HAS_OCCURRENCE = "MKV.hasOccurrence";
	public static final String HAS_THREAT = "MKV.hasThreat";
	public static final String HAS_ENDANGERMENT_REASON = "MKV.hasEndangermentReason";
	public static final String ENDANGERMENT = "MKV.endangerment";
	public static final String PUBLICATION = "MKV.publication";
	public static final String EDIT_NOTES = "MKV.editNotes";
	public static final String LAST_MODIFIED_BY = "MKV.lastModifiedBy";
	public static final String LAST_MODIFIED = "MKV.lastModified";
	public static final String RED_LIST_INDEX_CORRECTION = "MKV.redListIndexCorrection";
	public static final String RED_LIST_STATUS_NOTES = "MKV.redListStatusNotes";
	public static final String NE_MARK_NOTES = "Merkitty ei-arvioitavaksi pikatoiminnolla.";
	public static final String HAS_REGIONAL_STATUS = "MKV.hasRegionalStatus";
	public static final String REGIONAL_STATUS_STATUS = "MKV.regionalStatusStatus";
	public static final String REGIONAL_STATUS_AREA = "MKV.regionalStatusArea";
	public static final String REGIONAL_STATUS_CLASS = "MKV.regionalStatus";
	public static final String ENDANGERMENT_OBJECT_CLASS = "MKV.endangermentObject"; 
	public static final String CRITERIA_FOR_STATUS = "MKV.criteriaForStatus";
	public static final String CRITERIA_A = "MKV.criteriaA";
	public static final String CRITERIA_B = "MKV.criteriaB";
	public static final String CRITERIA_C = "MKV.criteriaC";
	public static final String CRITERIA_D = "MKV.criteriaD";
	public static final String CRITERIA_E = "MKV.criteriaE";
	public static final String STATUS_A = "MKV.statusA";
	public static final String STATUS_B = "MKV.statusB";
	public static final String STATUS_C = "MKV.statusC";
	public static final String STATUS_D = "MKV.statusD";
	public static final String STATUS_E = "MKV.statusE";
	public static final String IS_LOCKED = "MKV.locked";
	public static final String REMARKS = "MKV.remarks";
	public static final String LSA_RECOMMENDATION = "MKV.lsaRecommendation";
	public static final String RED_LIST_STATUS_MAX = "MKV.redListStatusMax";
	public static final String RED_LIST_STATUS_MIN = "MKV.redListStatusMin";
	public static final String INDIVIDUAL_COUNT_MAX = "MKV.individualCountMax";
	public static final String INDIVIDUAL_COUNT_MIN = "MKV.individualCountMin";

	public static final List<String> CRITERIAS = Utils.list("A", "B", "C", "D", "E");

	public static final Map<String, Integer> RED_LIST_STATUS_TO_INDEX;
	static {
		RED_LIST_STATUS_TO_INDEX = new HashMap<>();
		RED_LIST_STATUS_TO_INDEX.put("MX.iucnEX", 5);
		RED_LIST_STATUS_TO_INDEX.put("MX.iucnEW", 5);
		RED_LIST_STATUS_TO_INDEX.put("MX.iucnRE", 5);
		RED_LIST_STATUS_TO_INDEX.put("MX.iucnCR", 4);
		RED_LIST_STATUS_TO_INDEX.put("MX.iucnEN", 3);
		RED_LIST_STATUS_TO_INDEX.put("MX.iucnVU", 2);
		RED_LIST_STATUS_TO_INDEX.put("MX.iucnNT", 1);
		RED_LIST_STATUS_TO_INDEX.put("MX.iucnLC", 0);
		RED_LIST_STATUS_TO_INDEX.put("MX.iucnDD", null);
		RED_LIST_STATUS_TO_INDEX.put("MX.iucnNA", null);
		RED_LIST_STATUS_TO_INDEX.put("MX.iucnNE", null); 
	}

	private final RdfProperties evaluationProperties;
	private final Model evaluation;
	private Map<String, Occurrence> occurrences = null;
	private Map<String, IUCNRegionalStatus> regionalStatuses = null;
	private IUCNHabitatObject primaryHabitat;
	private List<IUCNHabitatObject> secondaryHabitats = null;
	private List<IUCNEndangermentObject> endangermentReasons = null;
	private List<IUCNEndangermentObject> threats = null;
	private boolean incompletelyLoaded = false;
	
	public IUCNEvaluation(Model evaluation, RdfProperties evaluationProperties) {
		this.evaluation = evaluation;
		this.evaluationProperties = evaluationProperties;
	}

	public void setPrimaryHabitat(IUCNHabitatObject habitat) {
		primaryHabitat = habitat;
	}

	public void addSecondaryHabitat(IUCNHabitatObject habitat) {
		if (secondaryHabitats == null) secondaryHabitats = new ArrayList<>();
		if (secondaryHabitats.contains(habitat)) return;
		secondaryHabitats.add(habitat);
		Collections.sort(secondaryHabitats);
	}

	public IUCNHabitatObject getPrimaryHabitat() {
		return primaryHabitat;
	}

	public List<IUCNHabitatObject> getSecondaryHabitats() {
		if (secondaryHabitats == null) return Collections.emptyList();
		return Collections.unmodifiableList(secondaryHabitats);
	}

	public boolean isLocked() {
		return "true".equals(getValue(IS_LOCKED));
	}

	public void addOccurrence(Occurrence occurrence) {
		if (occurrences == null) occurrences = new HashMap<>();
		if (occurrence.getYear() == null) occurrence.setYear(getEvaluationYear());
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

	public Collection<Occurrence> getOccurrences() {
		if (occurrences == null) return Collections.emptyList();
		return Collections.unmodifiableCollection(occurrences.values());
	}

	public void addRegionalStatus(IUCNRegionalStatus regionalStatus) {
		if (regionalStatuses == null) regionalStatuses = new HashMap<>();
		regionalStatuses.put(regionalStatus.getArea().toString(), regionalStatus);
	}

	public boolean hasRegionalStatus(String areaQname) {
		if (regionalStatuses == null) return false;
		return regionalStatuses.containsKey(areaQname);
	}

	public IUCNRegionalStatus getRegionalStatus(String areaQname) {
		if (regionalStatuses == null) return null;
		return regionalStatuses.get(areaQname);
	}

	public Collection<IUCNRegionalStatus> getRegionalStatuses() {
		if (regionalStatuses == null) return Collections.emptyList();
		return Collections.unmodifiableCollection(regionalStatuses.values());
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

	public boolean isVulnerable() {
		Integer index = getCalculatedIucnIndex();
		if (index == null) return false;
		return index >= 2;
	}

	public List<IUCNEndangermentObject> getEndangermentReasons() {
		if (endangermentReasons == null) return Collections.emptyList();
		return endangermentReasons;
	}

	public void addEndangermentReason(IUCNEndangermentObject endangermentReason) {
		if (endangermentReasons == null) endangermentReasons = new ArrayList<>();
		if (endangermentReasons.contains(endangermentReason)) return;
		endangermentReasons.add(endangermentReason);
		Collections.sort(endangermentReasons);
	}

	public List<IUCNEndangermentObject> getThreats() {
		if (threats == null) return Collections.emptyList();
		return threats;
	}

	public void addThreat(IUCNEndangermentObject threat) {
		if (threats == null) threats = new ArrayList<>();
		if (threats.contains(threat)) return;
		threats.add(threat);
		Collections.sort(threats);
	}

	public void copySpecifiedFieldsTo(IUCNEvaluation copyTarget) {
		copy(TYPE_OF_OCCURRENCE_IN_FINLAND, copyTarget);
		copy(DISTRIBUTION_AREA_MIN, copyTarget);
		copy(DISTRIBUTION_AREA_MAX, copyTarget);
		copy(OCCURRENCE_AREA_MIN, copyTarget);
		copy(OCCURRENCE_AREA_MAX, copyTarget);
		copy(OCCURRENCE_NOTES, copyTarget);

		for (Occurrence occurrence : this.getOccurrences()) {
			copyTarget.addOccurrence(copy(occurrence));
		}

		copyTarget.setPrimaryHabitat(copy(this.getPrimaryHabitat()));
		for (IUCNHabitatObject habitatObject : this.getSecondaryHabitats()) {
			copyTarget.addSecondaryHabitat(copy(habitatObject));
		}
		copy(HABITAT_GENERAL_NOTES, copyTarget);

		copy(GENERATION_AGE, copyTarget);
		copy(EVALUATION_PERIOD_LENGTH, copyTarget);
		copy(POPULATION_VARIES, copyTarget);
		copy(FRAGMENTED_HABITATS, copyTarget);
		copy(BORDER_GAIN, copyTarget);

		for (IUCNEndangermentObject endangermentObject : this.getEndangermentReasons()) {
			copyTarget.addEndangermentReason(copy(endangermentObject));
		}
		for (IUCNEndangermentObject endangermentObject : this.getThreats()) {
			copyTarget.addThreat(copy(endangermentObject));
		}

		copy(LAST_SIGHTING_NOTES, copyTarget);

		for (IUCNRegionalStatus regionalStatus : this.getRegionalStatuses()) {
			copyTarget.addRegionalStatus(copy(regionalStatus));
		}
	}

	private IUCNRegionalStatus copy(IUCNRegionalStatus regionalStatus) {
		return new IUCNRegionalStatus(null, regionalStatus.getArea(), regionalStatus.getStatus());
	}

	private IUCNEndangermentObject copy(IUCNEndangermentObject endangermentObject) {
		return new IUCNEndangermentObject(null, endangermentObject.getEndangerment(), endangermentObject.getOrder());
	}

	private IUCNHabitatObject copy(IUCNHabitatObject habitatObject) {
		if (habitatObject == null) return null;
		IUCNHabitatObject copy = new IUCNHabitatObject(null, habitatObject.getHabitat(), habitatObject.getOrder());
		for (Qname habitatSpecificType : habitatObject.getHabitatSpecificTypes()) {
			copy.addHabitatSpecificType(habitatSpecificType);
		}
		return copy;
	}

	private Occurrence copy(Occurrence occurrence) {
		return new Occurrence(null, occurrence.getArea(), occurrence.getStatus());
	}

	private void copy(String predicateQname, IUCNEvaluation copyTarget) {
		List<Statement> statements = this.getModel().getStatements(predicateQname);
		for (Statement statement : statements) {
			copyTarget.getModel().addStatement(copy(statement));
		}
	}

	private Statement copy(Statement statement) {
		if (statement.isLiteralStatement()) {
			ObjectLiteral literal = statement.getObjectLiteral();
			if (literal.hasLangcode()) {
				return new Statement(statement.getPredicate(), new ObjectLiteral(literal.getContent(), literal.getLangcode()));
			} else {
				return new Statement(statement.getPredicate(), new ObjectLiteral(literal.getContent()));
			}
		}
		return new Statement(statement.getPredicate(), new ObjectResource(statement.getObjectResource().getQname()));
	}

	public boolean hasRemarks() {
		return getValue(REMARKS) != null;
	}
	public String getRemarks() {
		StringBuilder b = new StringBuilder();
		for (String s : getValues(REMARKS)) {
			b.append(s).append("\n");
		}
		return b.toString();
	}

	public List<Statement> getRemarkSatements() {
		return evaluation.getStatements(REMARKS);
	}

	public boolean isIncompletelyLoaded() {
		return incompletelyLoaded;
	}

	public void setIncompletelyLoaded(boolean incompletelyLoaded) {
		this.incompletelyLoaded = incompletelyLoaded;
	}

}
