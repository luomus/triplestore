package fi.luomus.triplestore.taxonomy.iucn.model;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.RdfProperties;
import fi.luomus.commons.containers.rdf.RdfProperty;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.reporting.ErrorReporter;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.dao.TriplestoreDAO;

public class IUCNValidator {

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
			validationResult.setError("Tarkistuksissa tapahtui odottamaton virhe. Ylläpitoa on tiedotettu asiasta. ");
			errorReporter.report(e);
		}
		return validationResult;
	}

	private void tryToValidate(IUCNEvaluation givenData, IUCNEvaluation comparisonData, IUCNValidationResult validationResult) throws Exception {
		validateDataTypes(givenData, validationResult);
		if (givenData.isReady()) {
			validateRequiredFields(givenData, validationResult);
			validateEndangermentReason(givenData, validationResult);
			validateStatusChange(givenData, comparisonData, validationResult);
		}
		validateMinMaxPair("MKV.countOfOccurrencesMin", "MKV.countOfOccurrencesMax", INTEGER_COMPARATOR, givenData, validationResult);
		validateMinMaxPair("MKV.distributionAreaMin", "MKV.distributionAreaMax", INTEGER_COMPARATOR, givenData, validationResult);
		validateMinMaxPair("MKV.occurrenceAreaMin", "MKV.occurrenceAreaMax", INTEGER_COMPARATOR, givenData, validationResult);
		validateMinMaxPair("MKV.individualCountMin", "MKV.individualCountMax", INTEGER_COMPARATOR, givenData, validationResult);
		validateMinMaxPair("MKV.redListStatusMin", "MKV.redListStatusMax", IUCN_RANGE_COMPARATOR, givenData, validationResult);	
		validateCriteriaFormat(givenData, validationResult);
	}

	private void validateDataTypes(IUCNEvaluation givenData, IUCNValidationResult validationResult) throws Exception {
		RdfProperties properties = dao.getProperties(IUCNEvaluation.EVALUATION_CLASS);
		for (RdfProperty p : properties.getAllProperties()) {
			if (p.isIntegerProperty()) {
				for (Statement s : givenData.getModel().getStatements(p.getQname())) {
					if (notValidInteger(s.getObjectLiteral().getContent())) {
						String label = getLabel(p);
						validationResult.setError("Epäkelpo luku kentässä " + label + ": " + s.getObjectLiteral().getContent());
					}
				}
			} else if (p.isBooleanProperty()) {
				for (Statement s : givenData.getModel().getStatements(p.getQname())) {
					if (notValidBoolean(s.getObjectLiteral().getContent())) {
						String label = getLabel(p);
						validationResult.setError("Epäkelpo arvo kentässä " + label + ": " + s.getObjectLiteral().getContent());
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

	private void validateCriteriaFormat(IUCNEvaluation givenData, IUCNValidationResult validationResult) {
		validateCriteriaFormat(givenData.getValues("MKV.criteriaA"), "A", validationResult);
		validateCriteriaFormat(givenData.getValues("MKV.criteriaB"), "B", validationResult);
		validateCriteriaFormat(givenData.getValues("MKV.criteriaC"), "C", validationResult);
		validateCriteriaFormat(givenData.getValues("MKV.criteriaD"), "D", validationResult);
		validateCriteriaFormat(givenData.getValues("MKV.criteriaE"), "E", validationResult);
	}

	private void validateCriteriaFormat(List<String> values, String criteriaPrefix, IUCNValidationResult validationResult) {
		Set<String> validCriteria = VALID_CRITERIA.get(criteriaPrefix);
		for (String value : values) {
			if (!validCriteria.contains(value)) {
				//validationResult.setError("Kriteeri " + value +" ei ole sallittujen arvojen joukossa: " + validCriteria);
			}
		}
		
	}

	private void validateStatusChange(IUCNEvaluation givenData, IUCNEvaluation comparisonData, IUCNValidationResult validationResult) {
		List<String> statusChangeReasons = givenData.getValues("MKV.reasonForStatusChange");
		if (!statusChangeReasons.isEmpty()) {
			if (statusChangeReasons.contains("MKV.reasonForStatusChangeGenuine") || statusChangeReasons.contains("MKV.reasonForStatusChangeGenuineBeforePreviousEvaluation")) {
				if (statusChangeReasons.size() > 1) {
					validationResult.setError("Aitoa muutosta ja ei-aitoa muutosta ei saa merkitä yhtä aikaa");
					return;
				}
			}
		}
		
		if (comparisonData == null) return;
		
		String thisStatus = givenData.getIucnStatus();
		String prevStatus = comparisonData.getIucnStatus();
		if (!given(prevStatus) || !given(thisStatus)) return;
		
		Integer thisStatusOrder = IUCN_COMPARATOR_VALUES.get(thisStatus);
		Integer prevStatusOrder = IUCN_COMPARATOR_VALUES.get(prevStatus);
		if (thisStatusOrder == null || prevStatusOrder == null) return;
		
		if (!statusChangeReasons.isEmpty()) {
			if (thisStatusOrder == prevStatusOrder) {
				validationResult.setError("Muutoksen syytä ei saa antaa jos arvioinnin luokka ei ole muuttunut");
			}
		} else {
			if (thisStatusOrder != prevStatusOrder) {
				validationResult.setError("Muutoksen syy on annettava jos edellisen arvioinnin luokka ei ole sama kuin tämän arvioinnin luokka");
			}
		}
	}

	private void validateEndangermentReason(IUCNEvaluation givenData, IUCNValidationResult validationResult) throws Exception {
		String reason = givenData.getValue("MKV.endangermentReason");
		if (given(reason)) return;
		String status = givenData.getIucnStatus();
		Integer statusOrderValue = IUCN_COMPARATOR_VALUES.get(status);
		if (statusOrderValue == null) return;
		if (statusOrderValue >= ENDAGEREMENT_REASON_NEEDED_IF_STATUS_AT_LEAST) {
			validationResult.setError("Uhanalaisuuden syyt on määriteltävä uhanalaisuusluokalle " + getLabel(status));
		}
	}

	private boolean given(String s) {
		return s != null && s.length() > 0;
	}

	private Comparator<String> IUCN_RANGE_COMPARATOR = new Comparator<String>() {
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
				validationResult.setError("Arvovälin ala-arvo " + labelMin + " ei saa olla suurempi kuin yläarvo " + labelMax);
			}
		} catch (IllegalArgumentException e) {
			String invalidValue = e.getMessage();
			String valueLabel = getLabel(invalidValue);
			validationResult.setError("Arvoa " + valueLabel + " ei voi käyttää arvovälinä");
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
					validationResult.setError("Pakollinen tieto: " + label);
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
	private static final int ENDAGEREMENT_REASON_NEEDED_IF_STATUS_AT_LEAST = IUCN_COMPARATOR_VALUES.get("MX.iucnNT");
	
	private static final Map<String, Set<String>> VALID_CRITERIA = new HashMap<>();
	static {
		Set<String> a = Utils.set(
				"A1", "A1a", "A1b", "A1c", "A1d", "A1e", 
				"A2", "A2a", "A2b", "A2c", "A2d", "A2e", 
				"A3", "A3b", "A3c", "A3d", "A3e", 
				"A4", "A4a", "A4b", "A4c", "A4d", "A4e");
		Set<String> b = Utils.set(
				"B1", "B1a", "B1b", "B1b(i)");
		Set<String> c = Utils.set("");
		Set<String> d = Utils.set("");
		Set<String> e = Utils.set("");
		VALID_CRITERIA.put("A", a);
		VALID_CRITERIA.put("B", b);
		VALID_CRITERIA.put("C", c);
		VALID_CRITERIA.put("D", d);
		VALID_CRITERIA.put("E", e);
	}
}
