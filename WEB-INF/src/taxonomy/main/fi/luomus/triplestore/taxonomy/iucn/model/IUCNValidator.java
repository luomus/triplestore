package fi.luomus.triplestore.taxonomy.iucn.model;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.RdfProperties;
import fi.luomus.commons.containers.rdf.RdfProperty;
import fi.luomus.commons.reporting.ErrorReporter;
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
		if (givenData.isReady()) {
			validateRequiredFields(givenData, validationResult);
			validateEndangermentReason(givenData, validationResult);
			validateStatusChange(givenData, comparisonData, validationResult);
		}
		validateMinMaxPair("MKV.countOfOccurrencesMin", "MKV.countOfOccurrencesMax", INTEGER_COMPARATOR, givenData, validationResult);
		validateMinMaxPair("MKV.distributionAreaMin", "MKV.distributionAreaMax", INTEGER_COMPARATOR, givenData, validationResult);
		validateMinMaxPair("MKV.individualCountMin", "MKV.individualCountMax", INTEGER_COMPARATOR, givenData, validationResult);
		validateMinMaxPair("MKV.redListStatusMin", "MKV.redListStatusMax", IUCN_RANGE_COMPARATOR, givenData, validationResult);		
	}

	private void validateStatusChange(IUCNEvaluation givenData, IUCNEvaluation comparisonData, IUCNValidationResult validationResult) {
		String statusChangeReason = givenData.getValue("MKV.reasonForStatusChange");
		if (given(statusChangeReason)) return;
		if (comparisonData == null) return;
		String thisStatus = givenData.getIucnStatus();
		String prevStatus = comparisonData.getIucnStatus();
		if (!given(prevStatus) || !given(thisStatus)) return;
		Integer thisStatusOrder = IUCN_COMPARATOR_VALUES.get(thisStatus);
		Integer prevStatusOrder = IUCN_COMPARATOR_VALUES.get(prevStatus);
		if (thisStatusOrder == null || prevStatusOrder == null) return;
		if (thisStatusOrder != prevStatusOrder) {
			validationResult.setError("Muutoksen syy on annettava jos edellisen arvioinnin luokka ei ole sama kuin tämän arvioinnin luokka");
		}
	}

	private void validateEndangermentReason(IUCNEvaluation givenData, IUCNValidationResult validationResult) throws Exception {
		String reason = givenData.getValue("MKV.endangermentReason");
		if (given(reason)) return;
		String status = givenData.getIucnStatus();
		Integer statusOrderValue = IUCN_COMPARATOR_VALUES.get(status);
		if (statusOrderValue == null) return;
		if (statusOrderValue >= ENDAGEREMENT_REASON_NEEDED_IF_STATUS_AT_LEAST) {
			validationResult.setError("Uhkatekijät on määriteltävä uhanalaisuusluokalle " + getLabel(status));
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
					String label = p.getLabel().forLocale("fi");
					if (label == null) label = p.getQname().toString();
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
}
