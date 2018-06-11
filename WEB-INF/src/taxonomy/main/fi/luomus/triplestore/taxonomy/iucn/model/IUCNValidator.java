package fi.luomus.triplestore.taxonomy.iucn.model;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.RdfProperties;
import fi.luomus.commons.containers.rdf.RdfProperty;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.reporting.ErrorReporter;
import fi.luomus.commons.taxonomy.Occurrences.Occurrence;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.taxonomy.iucn.model.CriteriaFormatValidator.CriteriaValidationResult;
import fi.luomus.triplestore.taxonomy.iucn.model.CriteriaFormatValidator.MainCriteria;

public class IUCNValidator {

	private static final String KRITEERIEN_TARKISTUKSET = "Kriteerien tarkistukset: ";
	private final TriplestoreDAO dao;
	private final ErrorReporter errorReporter;

	public IUCNValidator(TriplestoreDAO dao, ErrorReporter errorReporter) {
		this.dao = dao;
		this.errorReporter = errorReporter;
	}

	public IUCNValidationResult validate(IUCNEvaluation givenData, IUCNEvaluation comparisonData) {
		IUCNValidationResult validationResult = new IUCNValidationResult();
		try {
			tryToValidate(givenData, comparisonData, validationResult);
		} catch (Exception e) {
			validationResult.setError("Tarkistuksissa tapahtui odottamaton virhe. Ylläpitoa on tiedotettu asiasta. ", null);
			errorReporter.report("Validation error report (handled in UI)", e);
		}
		return validationResult;
	}

	private void tryToValidate(IUCNEvaluation givenData, IUCNEvaluation comparisonData, IUCNValidationResult validationResult) throws Exception {
		validateInternals(givenData, validationResult);
		validateDataTypes(givenData, validationResult);
		if (givenData.isReady() || givenData.isReadyForComments()) {
			validateRequiredFields(givenData, validationResult);
			validateStatusChange(givenData, comparisonData, validationResult);
			validateRegionalEndangerment(givenData, validationResult);
			validateInvasive(givenData, validationResult);
			validate_Criteria_CriteriaStatus_Pair(givenData, validationResult);
			validateAdditionalDataRequirementsSpecificCriterias(givenData, validationResult);
			validateCriteriaRequirements(givenData, validationResult);
			validateOccurrences(givenData, validationResult);
			validateHabitatForStatus(givenData, validationResult);
			validateEndangermentReasonForStatus(givenData, validationResult);
			validateThreathsForStatus(givenData, validationResult);
			validateLsaForStatus(givenData, validationResult);
			validateCriteriaForStatusForStatus(givenData, validationResult);
			validateExternalImpactAndStatus(givenData, validationResult);
			validatePossiblyRE(givenData, validationResult);
			validateDDReason(givenData, validationResult);
			validatePopulationSize(givenData, validationResult);
		}

		validateHabitat(givenData.getPrimaryHabitat(), validationResult);
		for (IUCNHabitatObject habitatObject : givenData.getSecondaryHabitats()) {
			validateHabitat(habitatObject, validationResult);
		}

		validateMinMaxPair(IUCNEvaluation.OCCURRENCE_AREA_MIN, IUCNEvaluation.OCCURRENCE_AREA_MAX, INTEGER_COMPARATOR, givenData, validationResult);
		validateMinMaxPair(IUCNEvaluation.DISTRIBUTION_AREA_MIN, IUCNEvaluation.DISTRIBUTION_AREA_MAX, INTEGER_COMPARATOR, givenData, validationResult);
		validateMinMaxPair(IUCNEvaluation.INDIVIDUAL_COUNT_MIN, IUCNEvaluation.INDIVIDUAL_COUNT_MAX, INTEGER_COMPARATOR, givenData, validationResult);
		validateMinMaxPair(IUCNEvaluation.RED_LIST_STATUS_MIN, IUCNEvaluation.RED_LIST_STATUS_MAX, IUCN_RANGE_COMPARATOR, givenData, validationResult);	
		validateCriteriaFormat(givenData, validationResult);
		validateEvaluationPeriodLength(givenData, validationResult);
		validateValidCriteriaStatuses(givenData, validationResult);
		validateGlobalPercentage(givenData, validationResult);
	}

	private void validateCriteriaRequirements(IUCNEvaluation givenData, IUCNValidationResult validationResult) {
		String status = givenData.getIucnStatus();
		List<MainCriteria> criteria = null;

		try {
			criteria = CriteriaFormatValidator.parseCriteria(givenData.getValue(IUCNEvaluation.CRITERIA_FOR_STATUS));
		} catch (Exception e) {
			// criteria format validation errors are handled in other validations
			return;
		}

		for (MainCriteria c : criteria) {
			if (c.getMainCriteria().equals("E")) {
				validationResult.setError(KRITEERIEN_TARKISTUKSET + "Kriteeriä E ei käytetä", IUCNEvaluation.CRITERIA_FOR_STATUS);
			}
			if (c.getMainCriteria().equals("D")) {
				validationResult.setError(KRITEERIEN_TARKISTUKSET + "Kriteeriä D ei käytetä. Tarkenna D1 tai D2.", IUCNEvaluation.CRITERIA_FOR_STATUS);
			}
			if (c.getMainCriteria().startsWith("B")) {
				validateCriteriaBRequirementsForStatus(status, c, validationResult);
			}
			if (c.getMainCriteria().startsWith("D")) {
				validateCriteriaDRequirementsForStatus(status, c, validationResult);
			}
		}

	}

	private void validateCriteriaDRequirementsForStatus(String status, MainCriteria c, IUCNValidationResult validationResult) {
		if (CR_EN.contains(status) && c.getMainCriteria().equals("D2")) {
			validationResult.setError(KRITEERIEN_TARKISTUKSET + "Jos kriteeri on D2, luokka ei voi olla EN tai CR", IUCNEvaluation.RED_LIST_STATUS);
		}
	}

	private void validateCriteriaBRequirementsForStatus(String status, MainCriteria c, IUCNValidationResult validationResult) {
		if (CR_EN_VU.contains(status)) {
			if (c.getSubCriterias().size() < 2) {
				validationResult.setError(KRITEERIEN_TARKISTUKSET + "VU-CR luokat: Sekä kriteerissä B1 että B2 pitää molemmissa olla merkittynä vähintään kaksi alakriteeriä", IUCNEvaluation.RED_LIST_STATUS);
			}
		}
	}

	private void validatePopulationSize(IUCNEvaluation givenData, IUCNValidationResult validationResult) {
		Double min = d(givenData.getValue(IUCNEvaluation.INDIVIDUAL_COUNT_MIN));
		Double max = d(givenData.getValue(IUCNEvaluation.INDIVIDUAL_COUNT_MAX));
		if (min == null && max == null) return;
		if (min == null) min = 0.0;
		if (max == null) max = 0.0;
		max = Math.max(min, max);
		validateIndividualCountMax("MX.iucnVU", Utils.list("C1", "C2"), max, 10000, givenData, validationResult);
		validateIndividualCountMax("MX.iucnEN", Utils.list("C1", "C2"), max, 2500, givenData, validationResult);
		validateIndividualCountMax("MX.iucnCR", Utils.list("C1", "C2"), max, 250, givenData, validationResult);
		validateIndividualCountMax("MX.iucnVU", Utils.list("D", "D1"), max, 1000, givenData, validationResult);
		validateIndividualCountMax("MX.iucnEN", Utils.list("D", "D1"), max, 250, givenData, validationResult);
		validateIndividualCountMax("MX.iucnCR", Utils.list("D", "D1"), max, 50, givenData, validationResult);
	}

	private void validateIndividualCountMax(String status, List<String> criteria, Double individualCount, int limit, IUCNEvaluation givenData, IUCNValidationResult validationResult) {
		if (status.equals(givenData.getIucnStatus())) {
			String evalCriteria = givenData.getValue(IUCNEvaluation.CRITERIA_FOR_STATUS);
			if (evalCriteria == null) return;
			if (hasCriteria(evalCriteria, criteria)) {
				if (individualCount > limit) {
					validationResult.setError(status.replace("MX.iucn", "") + " luokalla ja kriteereillä " + evalCriteria + " yksilömäärän on oltava alle " + limit, IUCNEvaluation.INDIVIDUAL_COUNT_MAX);
				}
			}
		}
	}

	private boolean hasCriteria(String evalCriteria, List<String> criteria) {
		for (String c : criteria) {
			if (evalCriteria.contains(c)) {
				return true;
			}
		}
		return false;
	}

	private void validateDDReason(IUCNEvaluation givenData, IUCNValidationResult validationResult) {
		if ("MX.iucnDD".equals(givenData.getIucnStatus())) {
			if (!givenData.hasValue(IUCNEvaluation.DD_REASON)) {
				validationResult.setError("DD syy on ilmoitettava", IUCNEvaluation.DD_REASON);
			}
		}
	}

	private void validatePossiblyRE(IUCNEvaluation givenData, IUCNValidationResult validationResult) {
		if (!givenData.hasValue(IUCNEvaluation.POSSIBLY_RE)) return;
		Set<String> allowed = "D2".equals(givenData.getValue(IUCNEvaluation.CRITERIA_FOR_STATUS)) ? Utils.set("MX.iucnCR", "MX.iucnDD", "MX.iucnVU") : Utils.set("MX.iucnCR", "MX.iucnDD");
		if (!allowed.contains(givenData.getIucnStatus())) {
			validationResult.setError("Mahdollisesti hävinneeksi saa ilmoittaa ainoastaan luokkia CR, DD tai kriteerillä D2 myös luokassa VU", IUCNEvaluation.POSSIBLY_RE);
		}
	}

	private void validateGlobalPercentage(IUCNEvaluation givenData, IUCNValidationResult validationResult) {
		if (givenData.hasValue(IUCNEvaluation.PERCENTAGE_OF_GLOBAL_POPULATION)) {
			try {
				double d = Double.valueOf(givenData.getValue(IUCNEvaluation.PERCENTAGE_OF_GLOBAL_POPULATION));
				if (d < 0 || d > 100) {
					validationResult.setError("Oltava väliltä 0-100%", IUCNEvaluation.PERCENTAGE_OF_GLOBAL_POPULATION);
				}
			} catch (Exception e) {}
		}
	}

	private void validateValidCriteriaStatuses(IUCNEvaluation givenData, IUCNValidationResult validationResult) throws Exception {
		for (String criteria : IUCNEvaluation.CRITERIAS) {
			String status = givenData.getValue("MKV.status"+criteria);
			if (!given(status)) continue;
			validateValidCriteriaStatus(status, "MKV.status"+criteria, validationResult);
		}
	}

	private void validateValidCriteriaStatus(String statusQname, String fieldQname, IUCNValidationResult validationResult) throws Exception {
		Integer i = IUCN_COMPARATOR_VALUES.get(statusQname);
		if (i == null) {
			String statusLabel = getLabel(statusQname);
			validationResult.setError("Luokkaa " + statusLabel + " ei voi käyttää kriteerin aiheuttamana luokkana", fieldQname);
		}
	}

	private void validateEvaluationPeriodLength(IUCNEvaluation givenData, IUCNValidationResult validationResult) {
		String val = givenData.getValue(IUCNEvaluation.EVALUATION_PERIOD_LENGTH);
		if (!given(val)) return;
		try {
			int i = Integer.valueOf(val);
			if (i > 100 || i < 10) {
				validationResult.setError("Tarkastelujakson pituus on oltava väliltä 10-100", IUCNEvaluation.EVALUATION_PERIOD_LENGTH);
			}
		} catch (Exception e) {
			// Invalid number validated elsewhere
		}
	}

	private static final Set<String> CR_EN = Utils.set("MX.iucnCR", "MX.iucnEN");
	private static final Set<String> CR_EN_VU = Utils.set("MX.iucnCR", "MX.iucnEN", "MX.iucnVU");
	private static final Set<String> CR_EN_VU_NT = Utils.set("MX.iucnCR", "MX.iucnEN", "MX.iucnVU", "MX.iucnNT");
	private static final Set<String> CR_EN_VU_NT_LC = Utils.set("MX.iucnCR", "MX.iucnEN", "MX.iucnVU", "MX.iucnNT", "MX.iucnLC");
	private static final Set<String> RE_CR_EN_VU_NT = Utils.set("MX.iucnRE", "MX.iucnCR", "MX.iucnEN", "MX.iucnVU", "MX.iucnNT");

	private static final Set<String> LSA_CAN_GIVE_STATUSES = CR_EN_VU;
	private static final Set<String> CRITERIA_FOR_STATUS_REQUIRED_STATUSES = CR_EN_VU_NT;
	private static final Set<String> THREATHS_REQUIRED_STATUSES = CR_EN_VU_NT;
	private static final Set<String> ENDANGERMENTREASON_REQUIRED_STATUSES = RE_CR_EN_VU_NT;
	private static final Set<String> CRITERIA_ENDANGERMENTREASON_NOT_REQUIRED = Utils.set("A3", "B1", "B2", "C2");
	private static final Set<String> PRIMARY_HABITAT_REQUIRED_STATUSES = CR_EN_VU_NT_LC;
	private static final Set<String> OCCURRENCES_REQUIRED_STATUSES = CR_EN_VU_NT;
	private static final Set<String> EXTERNAL_IMPACT_NOT_ALLOVED_STATUSES = Utils.set("MX.iucnEX", "MX.iucnEW", "MX.iucnRE", "MX.iucnDD", "MX.iucnNA", "MX.iucnNE");

	private void validateExternalImpactAndStatus(IUCNEvaluation givenData, IUCNValidationResult validationResult) {
		if (given(givenData.getExternalImpact())) {
			if (EXTERNAL_IMPACT_NOT_ALLOVED_STATUSES.contains(givenData.getIucnStatus())) {
				validationResult.setError("Luokan alennusta/korotusta ei saa käyttää luokille DD, NA, NE, RE, EW, EX", IUCNEvaluation.EXTERNAL_IMPACT);
			}
		}
	}

	private void validateLsaForStatus(IUCNEvaluation givenData, IUCNValidationResult validationResult) {
		String lsaRec = givenData.getValue(IUCNEvaluation.LSA_RECOMMENDATION);
		if (!"true".equals(lsaRec)) return;
		String status = givenData.getIucnStatus();
		if (!LSA_CAN_GIVE_STATUSES.contains(status)) {
			validationResult.setError("Erityisesti suojeltavaksi voi ehdottaa vain luokkaan VU-CR arvioituja", IUCNEvaluation.LSA_RECOMMENDATION);
		}
	}

	private void validateCriteriaForStatusForStatus(IUCNEvaluation givenData, IUCNValidationResult validationResult) {
		String status = givenData.getIucnStatus();
		if (CRITERIA_FOR_STATUS_REQUIRED_STATUSES.contains(status)) {
			if (!given(givenData.getValue(IUCNEvaluation.CRITERIA_FOR_STATUS))) {
				validationResult.setError("Luokkaan johtaneet kriteerit on täytettävä luokille NT-CR", IUCNEvaluation.CRITERIA_FOR_STATUS);
			}
		}
	}

	private void validateThreathsForStatus(IUCNEvaluation givenData, IUCNValidationResult validationResult) {
		String status = givenData.getIucnStatus();
		if (THREATHS_REQUIRED_STATUSES.contains(status)) {
			if (givenData.getThreats().isEmpty()) {
				validationResult.setError("Uhkatekijät on täytettävä luokille NT-CR", IUCNEvaluation.HAS_THREAT);
			}
		}
	}

	private void validateEndangermentReasonForStatus(IUCNEvaluation givenData, IUCNValidationResult validationResult) {
		String status = givenData.getIucnStatus();
		if (ENDANGERMENTREASON_REQUIRED_STATUSES.contains(status)) {
			if (endangermentNotRequiredForCriteria(givenData)) return;
			if (givenData.getEndangermentReasons().isEmpty()) {
				validationResult.setError("Uhanalaisuuden syyt on täytettävä luokille NT-RE", IUCNEvaluation.HAS_ENDANGERMENT_REASON);
			}
		}
	}

	private boolean endangermentNotRequiredForCriteria(IUCNEvaluation givenData) {
		String criteria = givenData.getValue(IUCNEvaluation.CRITERIA_FOR_STATUS);
		if (!given(criteria)) return false;
		for (String allowedCriteria : CRITERIA_ENDANGERMENTREASON_NOT_REQUIRED) {
			if (criteria.startsWith(allowedCriteria)) return true;
		}
		return false;
	}



	private void validateHabitatForStatus(IUCNEvaluation givenData, IUCNValidationResult validationResult) {
		String status = givenData.getIucnStatus();
		if (PRIMARY_HABITAT_REQUIRED_STATUSES.contains(status)) {
			if (givenData.getPrimaryHabitat() == null) {
				validationResult.setError("Ensisijainen elinympäristö on täytettävä luokille LC-CR", IUCNEvaluation.PRIMARY_HABITAT);
			}
		}
	}

	private void validateOccurrences(IUCNEvaluation givenData, IUCNValidationResult validationResult) {
		String status = givenData.getIucnStatus();
		if (OCCURRENCES_REQUIRED_STATUSES.contains(status)) {
			if (givenData.getOccurrences().isEmpty()) {
				validationResult.setError("Esiintymisalueet on täytettävä luokille NT-CR", IUCNEvaluation.HAS_OCCURRENCE);
			}
		}
	}

	private void validateAdditionalDataRequirementsSpecificCriterias(IUCNEvaluation givenData, IUCNValidationResult validationResult) {
		String criterias = parseCriterias(givenData);

		if (criterias.contains("B1")) {
			requiredWhenCriteriaB1Given(givenData, validationResult);
		}
		if (criterias.contains("B2")) {
			requiredWhenCriteriaB2Given(givenData, validationResult);
		}
		if (criterias.contains("A")) {
			requiredWhenCriteriaAGiven(givenData, validationResult);
		}
	}

	private String parseCriterias(IUCNEvaluation givenData) {
		String criterias = givenData.getValue(IUCNEvaluation.CRITERIA_FOR_STATUS);
		if (criterias == null) {
			criterias = "";
		} else {
			criterias = parseCriterias(criterias);
		}
		for (String criteriaPostfix : IUCNEvaluation.CRITERIAS) {
			String criteria = givenData.getValue("MKV.criteria"+criteriaPostfix);
			if (given(criteria)) {
				criterias = parseCriterias(criterias);
			}
		}
		return criterias;
	}

	private String parseCriterias(String criterias) {
		List<MainCriteria> parsed = CriteriaFormatValidator.parseCriteria(criterias);
		for (MainCriteria c : parsed) {
			criterias += c.getMainCriteria();
		}
		return criterias;
	}

	private void requiredWhenCriteriaAGiven(IUCNEvaluation givenData, IUCNValidationResult validationResult) {
		if (!given(givenData.getValue(IUCNEvaluation.EVALUATION_PERIOD_LENGTH))) {
			validationResult.setError("Tarkastelujakson pituus on ilmoitettava käytettäessä kriteeriä A", IUCNEvaluation.EVALUATION_PERIOD_LENGTH);
		}
	}

	private void requiredWhenCriteriaB1Given(IUCNEvaluation givenData, IUCNValidationResult validationResult) {
		String min = givenData.getValue(IUCNEvaluation.DISTRIBUTION_AREA_MIN);
		String max = givenData.getValue(IUCNEvaluation.DISTRIBUTION_AREA_MAX);
		if (!given(min) && !given(max)) {
			validationResult.setError("Levinneisyysalueen koko on ilmoitettava käytettäessä kriteeriä B1", IUCNEvaluation.DISTRIBUTION_AREA_MIN);
			validationResult.addErrorField(IUCNEvaluation.DISTRIBUTION_AREA_MAX);
		} else {
			Double dMin = d(min);
			Double dMax = d(max);
			validateAreaMax(givenData, "MX.iucnVU", 20000, dMin, dMax, IUCNEvaluation.DISTRIBUTION_AREA_MAX, validationResult);
			validateAreaMax(givenData, "MX.iucnEN", 5000, dMin, dMax, IUCNEvaluation.DISTRIBUTION_AREA_MAX, validationResult);
			validateAreaMax(givenData, "MX.iucnCR", 100, dMin, dMax, IUCNEvaluation.DISTRIBUTION_AREA_MAX, validationResult);
		}
	}

	private void requiredWhenCriteriaB2Given(IUCNEvaluation givenData, IUCNValidationResult validationResult) {
		String min = givenData.getValue(IUCNEvaluation.OCCURRENCE_AREA_MIN);
		String max = givenData.getValue(IUCNEvaluation.OCCURRENCE_AREA_MAX);
		if (!given(min) && !given(max)) {
			validationResult.setError("Esiintymisalueen koko on ilmoitettava käytettäessä kriteeriä B2", IUCNEvaluation.OCCURRENCE_AREA_MIN);
			validationResult.addErrorField(IUCNEvaluation.OCCURRENCE_AREA_MAX);
		} else {
			Double dMin = d(min);
			Double dMax = d(max);
			validateAreaMax(givenData, "MX.iucnVU", 2000, dMin, dMax, IUCNEvaluation.OCCURRENCE_AREA_MAX, validationResult);
			validateAreaMax(givenData, "MX.iucnEN", 500, dMin, dMax, IUCNEvaluation.OCCURRENCE_AREA_MAX, validationResult);
			validateAreaMax(givenData, "MX.iucnCR", 10, dMin, dMax, IUCNEvaluation.OCCURRENCE_AREA_MAX, validationResult);
		}
	}

	private void validateAreaMax(IUCNEvaluation givenData, String status, int limit, Double dMin, Double dMax, String field, IUCNValidationResult validationResult) {
		String messageField = field.equals(IUCNEvaluation.OCCURRENCE_AREA_MAX) ? "esiintymisalueen koko" : "levinneisyysalueen koko";
		if (status.equals(givenData.getIucnStatus())) {
			if (dMin != null && dMin > limit) {
				validationResult.setError(status.replace("MX.iucn", "") + " luokalla "+messageField+" saa olla enitään " +limit+" km²", field);
				return;
			}
			if (dMax != null && dMax > limit) {
				validationResult.setError(status.replace("MX.iucn", "") + " luokalla "+messageField+" saa olla enitään " +limit+" km²", field);
			}
		}
	}

	private Double d(String s) {
		if (notValidDecimal(s)) return null;
		try {
			return Double.valueOf(s);
		} catch (Exception e) {
			return null;
		}
	}

	private void validate_Criteria_CriteriaStatus_Pair(IUCNEvaluation givenData, IUCNValidationResult validationResult) {
		for (String criteriaPostfix : IUCNEvaluation.CRITERIAS) {
			String criteria = givenData.getValue("MKV.criteria"+criteriaPostfix);
			String criteriaStatus = givenData.getValue("MKV.status"+criteriaPostfix);
			if ("MX.iucnLC".equals(criteriaStatus)) return;
			if (given(criteria) || given(criteriaStatus)) {
				if (!given(criteria) || !given(criteriaStatus)) {
					validationResult.setError("Kriteeri " + criteriaPostfix + " ja siitä seuraava luokka on annettava jos toinen tiedoista annetaan", "MKV.status"+criteriaPostfix);
					validationResult.addErrorField("MKV.criteria"+criteriaPostfix);
				}
			}
		}
	}

	private void validateInternals(IUCNEvaluation givenData, IUCNValidationResult validationResult) {
		if (!IUCNEvaluation.EVALUATION_CLASS.equals(givenData.getModel().getType())) {
			validationResult.setError("Ohjelmointivirhe: rdf:type puuttuu!", null);
		}
		if (!given(givenData.getValue(IUCNEvaluation.EVALUATED_TAXON))) {
			validationResult.setError("Ohjelmointivirhe: " + IUCNEvaluation.EVALUATED_TAXON + " puuttuu!", null);
		}
		if (!given(givenData.getValue(IUCNEvaluation.EVALUATION_YEAR))) {
			validationResult.setError("Ohjelmointivirhe: " + IUCNEvaluation.EVALUATION_YEAR + " puuttuu!", null);
		}
		if (!given(givenData.getValue(IUCNEvaluation.STATE))) {
			validationResult.setError("Ohjelmointivirhe: " + IUCNEvaluation.STATE + " puuttuu!", null);
		}

		for (Occurrence o : givenData.getOccurrences()) {
			if (o.getYear() == null || o.getYear().intValue() != givenData.getEvaluationYear()) {
				validationResult.setError("Ohjelmointivirhe: Occurrences vuosi ei ole kunnossa", IUCNEvaluation.HAS_OCCURRENCE);
			}
			if (o.getStatus() == null) {
				validationResult.setError("Jokin esiintymisarvo on ilmoitettava jos RT on merkitty", null);
			} else if (o.getThreatened() != null && o.getThreatened() && !ALLOWED_FOR_RT.contains(o.getStatus())) {
				validationResult.setError("Tämä esiintymisarvo ei ole sallittu RT-arvon kanssa", null);
			}
		}
	}

	private static final Set<Qname> ALLOWED_FOR_RT = 
			Utils.set(new Qname("MX.typeOfOccurrenceOccurs"), new Qname("MX.typeOfOccurrenceAnthropogenic"), new Qname("MX.typeOfOccurrenceUncertain"));

	private void validateInvasive(IUCNEvaluation givenData, IUCNValidationResult validationResult) {
		String status = givenData.getValue(IUCNEvaluation.RED_LIST_STATUS);
		if (!given(status)) return;
		String statusInFinland = givenData.getValue(IUCNEvaluation.TYPE_OF_OCCURRENCE_IN_FINLAND);
		if ("MX.typeOfOccurrenceAnthropogenic".equals(statusInFinland)) {
			if (!"MX.iucnNA".equals(status)) {
				validationResult.setError("Vieraslajille ainut sallittu luokka on NA", IUCNEvaluation.RED_LIST_STATUS);
				validationResult.addErrorField(IUCNEvaluation.TYPE_OF_OCCURRENCE_IN_FINLAND);
			}
		}
	}

	private void validateHabitat(IUCNHabitatObject habitatObject, IUCNValidationResult validationResult) {
		if (habitatObject == null) return;
		if (habitatObject.getHabitatSpecificTypes().isEmpty()) return;
		if (!given(habitatObject.getHabitat())) {
			validationResult.setError("Elinympäristön lisämerkintää ei saa antaa jos varsinaista elinympäristö ei ole annettu.", null);
		}
	}

	private boolean given(Qname q) {
		return q != null && q.isSet();
	}

	private void validateRegionalEndangerment(IUCNEvaluation givenData, IUCNValidationResult validationResult) {
		String status = givenData.getIucnStatus();
		if (!given(status)) return;
		if ("MX.iucnLC".equals(status) || "MX.iucnNT".equals(status)) return;
		if (givenData.getOccurrences().isEmpty()) return;
		for (Occurrence o : givenData.getOccurrences()) {
			if ("MX.typeOfOccurrenceOccursButThreatened".equals(o.getStatus().toString())) {
				validationResult.setError("Alueelliseesti uhanalaiseksi voi merkitä vain luokkiin LC ja NT määriteltyjä lajeja. Tätä uhanalaisemmat lajit ovat automaattisesti alueellisesti uhanalaisia.", null);
				return;
			}
		}
	}

	private void validateDataTypes(IUCNEvaluation givenData, IUCNValidationResult validationResult) throws Exception {
		RdfProperties properties = dao.getProperties(IUCNEvaluation.EVALUATION_CLASS);
		for (Statement s : givenData.getModel().getStatements()) {
			if (s.isLiteralStatement()) {
				int length = Utils.countOfUTF8Bytes(s.getObjectLiteral().getUnsanitazedContent());
				if (length > 4000) {
					String label = getLabel(properties.getProperty(s.getPredicate())); 
					validationResult.setError("Liian pitkä teksti kentässä " + label, s.getPredicate().toString());
				}
			}
		}
		for (RdfProperty p : properties.getAllProperties()) {
			if (p.isIntegerProperty()) {
				for (Statement s : givenData.getModel().getStatements(p.getQname())) {
					if (notValidInteger(s.getObjectLiteral().getContent())) {
						String label = getLabel(p);
						validationResult.setError("Epäkelpo luku kentässä " + label + ": " + s.getObjectLiteral().getContent(), p.getQname().toString());
					}
				}
			} else if (p.isDecimalProperty()) {
				for (Statement s : givenData.getModel().getStatements(p.getQname())) {
					if (notValidDecimal(s.getObjectLiteral().getContent())) {
						String label = getLabel(p);
						validationResult.setError("Epäkelpo luku kentässä " + label + ": " + s.getObjectLiteral().getContent(), p.getQname().toString());
					}
				}
			} else if (p.isBooleanProperty()) {
				for (Statement s : givenData.getModel().getStatements(p.getQname())) {
					if (notValidBoolean(s.getObjectLiteral().getContent())) {
						String label = getLabel(p);
						validationResult.setError("Epäkelpo arvo kentässä " + label + ": " + s.getObjectLiteral().getContent(), p.getQname().toString());
					}
				}
			} else if (p.hasRangeValues()) {
				for (Statement s : givenData.getModel().getStatements(p.getQname())) {
					if (s.getObjectResource() == null) {
						validationResult.setError("Ohjelmointivirhe: Literaali asetettu muuttujalle " + s.getPredicate().getQname() + " jonka pitäisi olla joukossa " + p.getRange().getQname(), s.getPredicate().getQname());
					} else {
						try {
							p.getRange().getValueFor(s.getObjectResource().getQname());
						} catch (Exception e) {
							validationResult.setError("Ohjelmointivirhe: Virheellinen arvo " + s.getObjectResource().getQname() + " muuttujalle " + s.getPredicate().getQname(), s.getPredicate().getQname());
						}
					}
				}

			}
		}
	}

	private boolean notValidBoolean(String content) {
		if ("true".equals(content)) return false;
		if ("false".equals(content)) return false;
		return true;
	}

	private String getLabel(RdfProperty p) {
		String label = p.getLabel().forLocale("fi");
		if (label == null) label = p.getQname().toString();
		return label;
	}

	private boolean notValidInteger(String content) {
		try {
			Integer.valueOf(content);
			return false;
		} catch (Exception e) {
			return true;
		}
	}

	private boolean notValidDecimal(String content) {
		try {
			Double.valueOf(content);
			return false;
		} catch (Exception e) {
			return true;
		}
	}

	private void validateCriteriaFormat(IUCNEvaluation givenData, IUCNValidationResult validationResult) {
		for (String criteria : IUCNEvaluation.CRITERIAS) {
			validateCriteriaFormat(givenData.getValue("MKV.criteria"+criteria), criteria, validationResult);
		}

		String criteriaForStatus = givenData.getValue(IUCNEvaluation.CRITERIA_FOR_STATUS);
		if (given(criteriaForStatus)) {
			CriteriaValidationResult result = CriteriaFormatValidator.validateJoined(criteriaForStatus);
			if (!result.isValid()) {
				validationResult.setError(result.getErrorMessage(), IUCNEvaluation.CRITERIA_FOR_STATUS);
			}
		}
	}

	private void validateCriteriaFormat(String value, String criteriaPostfix, IUCNValidationResult validationResult) {
		if (!given(value)) return;
		CriteriaValidationResult result = CriteriaFormatValidator.forCriteria(criteriaPostfix).validate(value); 
		if (!result.isValid()) {
			validationResult.setError(result.getErrorMessage(), "MKV.criteria"+criteriaPostfix);
		}
	}

	private void validateStatusChange(IUCNEvaluation givenData, IUCNEvaluation comparisonData, IUCNValidationResult validationResult) {
		List<String> statusChangeReasons = givenData.getValues(IUCNEvaluation.REASON_FOR_STATUS_CHANGE);
		if (!statusChangeReasons.isEmpty()) {
			if (statusChangeReasons.contains("MKV.reasonForStatusChangeGenuine") || statusChangeReasons.contains("MKV.reasonForStatusChangeGenuineBeforePreviousEvaluation")) {
				if (statusChangeReasons.size() > 1) {
					validationResult.setError("Aitoa muutosta ja ei-aitoa muutosta ei saa merkitä yhtä aikaa", IUCNEvaluation.REASON_FOR_STATUS_CHANGE);
					return;
				}
			}
		}

		if (comparisonData == null) return;

		String thisStatus = givenData.getIucnStatus();
		String prevStatus = comparisonData.getIucnStatus();
		if (!given(prevStatus) || !given(thisStatus)) return;

		if (!statusChangeReasons.isEmpty()) {
			if (thisStatus.equals(prevStatus)) {
				validationResult.setError("Muutoksen syytä ei saa antaa jos arvioinnin luokka ei ole muuttunut", IUCNEvaluation.REASON_FOR_STATUS_CHANGE);
			}
			return;
		}

		if (thisStatus.equals(prevStatus)) return;

		if (prevStatus.equals("MX.iucnNE") || prevStatus.equals("MX.iucnNA")) return;
		if (thisStatus.equals("MX.iucnNE") || thisStatus.equals("MX.iucnNA")) return;

		validationResult.setError("Muutoksen syy on annettava jos edellisen arvioinnin luokka ei ole sama kuin tämän arvioinnin luokka", IUCNEvaluation.REASON_FOR_STATUS_CHANGE);
	}

	private boolean given(String s) {
		return s != null && s.length() > 0;
	}

	private static final Comparator<String> IUCN_RANGE_COMPARATOR = new Comparator<String>() {
		@Override
		public int compare(String o1, String o2) {
			Integer i1 = IUCN_COMPARATOR_VALUES.get(o1);
			Integer i2 = IUCN_COMPARATOR_VALUES.get(o2);
			if (i1 == null) throw new IllegalArgumentException(o1);
			if (i2 == null) throw new IllegalArgumentException(o2);
			return i1.compareTo(i2);
		}
	};


	private void validateMinMaxPair(String minField, String maxField, Comparator<String> comparator, IUCNEvaluation givenData, IUCNValidationResult validationResult) throws Exception {
		String minVal = givenData.getValue(minField);
		String maxVal = givenData.getValue(maxField);
		if (minVal == null || maxVal == null) return;
		try {
			int c = comparator.compare(minVal, maxVal);
			if (c > 0) {
				String labelMin = getLabel(minVal);
				String labelMax = getLabel(maxVal);
				validationResult.setError("Arvovälin ala-arvo " + labelMin + " ei saa olla suurempi kuin yläarvo " + labelMax, minField);
				validationResult.addErrorField(maxField);
			}
		} catch (IllegalArgumentException e) {
			String invalidField = e.getMessage();
			String valueLabel = getLabel(invalidField);
			validationResult.setError("Arvoa " + valueLabel + " ei voi käyttää arvovälinä", invalidField);
		}
	}

	private String getLabel(String property) throws Exception {
		String valueLabel = dao.getProperty(new Predicate(property)).getLabel().forLocale("fi");
		if (valueLabel == null) return property;
		return "\"" + valueLabel + "\"";
	}

	private static final Comparator<String> INTEGER_COMPARATOR = new Comparator<String>() {
		@Override
		public int compare(String o1, String o2) {
			return Integer.valueOf(o1).compareTo(Integer.valueOf(o2));
		}
	};


	private void validateRequiredFields(IUCNEvaluation givenData, IUCNValidationResult validationResult) throws Exception {
		RdfProperties properties = dao.getProperties(IUCNEvaluation.EVALUATION_CLASS);
		for (RdfProperty p : properties.getAllProperties()) {
			if (p.isRequired()) {
				if (!givenData.getModel().hasStatements(p.getQname())) {
					String label = getLabel(p);
					validationResult.setError("Pakollinen tieto: " + label, p.getQname().toString());
				}
			}
		}
	}

	private static final Map<String, Integer> IUCN_COMPARATOR_VALUES;
	static {
		IUCN_COMPARATOR_VALUES = new HashMap<>();
		IUCN_COMPARATOR_VALUES.put("MX.iucnEX", 8);
		IUCN_COMPARATOR_VALUES.put("MX.iucnEW", 7);
		IUCN_COMPARATOR_VALUES.put("MX.iucnRE", 6);
		IUCN_COMPARATOR_VALUES.put("MX.iucnCR", 5);
		IUCN_COMPARATOR_VALUES.put("MX.iucnEN", 4);
		IUCN_COMPARATOR_VALUES.put("MX.iucnVU", 3);
		IUCN_COMPARATOR_VALUES.put("MX.iucnNT", 2);
		IUCN_COMPARATOR_VALUES.put("MX.iucnLC", 1);
		IUCN_COMPARATOR_VALUES.put("MX.iucnDD", null);
		IUCN_COMPARATOR_VALUES.put("MX.iucnNA", null);
		IUCN_COMPARATOR_VALUES.put("MX.iucnNE", null);
	}

}
