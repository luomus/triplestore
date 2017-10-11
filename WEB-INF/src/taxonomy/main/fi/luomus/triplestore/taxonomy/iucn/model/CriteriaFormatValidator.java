package fi.luomus.triplestore.taxonomy.iucn.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import fi.luomus.commons.utils.Utils;

public class CriteriaFormatValidator {

	private static final CriteriaValidationResult VALID = new CriteriaValidationResult();

	public static class CriteriaValidationResult {
		private final String error;
		public CriteriaValidationResult() { 
			this(null); 
		}
		public CriteriaValidationResult(String error) {
			this.error = error;
		}
		public boolean isValid() {
			return error == null;
		}
		public String getErrorMessage() {
			return error;
		}
	}

	private static final Map<String, CriteriaFormatValidator> VALIDATORS = new LinkedHashMap<>(); 
	static {
		CriteriaFormatValidator a = new CriteriaFormatValidator();
		a.addMainCriteria("A1").addSubCriterias('a', 'b', 'c', 'd', 'e');
		a.addMainCriteria("A2").addSubCriterias('a', 'b', 'c', 'd', 'e');
		a.addMainCriteria("A3").addSubCriterias('b', 'c', 'd', 'e');
		a.addMainCriteria("A4").addSubCriterias('a', 'b', 'c', 'd', 'e');

		CriteriaFormatValidator b = new CriteriaFormatValidator();
		b.addMainCriteria("B1").addSubCriterias('a', 'b', 'c');
		b.getMainCriteria("B1").getSubCriteria('b').addSpecifications("i", "ii", "iii", "iv", "v");
		b.getMainCriteria("B1").getSubCriteria('c').addSpecifications("i", "ii", "iii", "iv");

		b.addMainCriteria("B2").addSubCriterias('a', 'b', 'c');
		b.getMainCriteria("B2").getSubCriteria('b').addSpecifications("i", "ii", "iii", "iv", "v");
		b.getMainCriteria("B2").getSubCriteria('c').addSpecifications("i", "ii", "iii", "iv");

		CriteriaFormatValidator c = new CriteriaFormatValidator();
		c.addMainCriteria("C1");
		c.addMainCriteria("C2").addSubCriterias('a', 'b');
		c.getMainCriteria("C2").getSubCriteria('a').addSpecifications("i", "ii");

		CriteriaFormatValidator d = new CriteriaFormatValidator();
		d.addMainCriteria("D1");
		d.addMainCriteria("D2");

		CriteriaFormatValidator e = new CriteriaFormatValidator();
		e.addMainCriteria("E");

		VALIDATORS.put("A", a);
		VALIDATORS.put("B", b);
		VALIDATORS.put("C", c);
		VALIDATORS.put("D", d);
		VALIDATORS.put("E", e);
	}

	public static class MainCriteria {

		private final List<SubCriteria> subCriterias = new ArrayList<>();
		private final String mainCriteria;

		public MainCriteria(String mainCriteria) {
			this.mainCriteria = mainCriteria;
		}

		public SubCriteria getSubCriteria(char subCriteria) {
			for (SubCriteria sub : subCriterias) {
				if (sub.getSubCriteria() == subCriteria) {
					return sub;
				}
			}
			return null;
		}

		public void addSubCriteria(SubCriteria subCriteria) {
			subCriterias.add(subCriteria);
		}

		public MainCriteria addSubCriterias(char ... subCriteria) {
			int i = 1;
			for (char c : subCriteria) {
				subCriterias.add(new SubCriteria(c, i++));	
			}
			return this;
		}

		public String getMainCriteria() {
			return mainCriteria;
		}

		public boolean hasSubCriterias() {
			return !subCriterias.isEmpty();
		}

		public String subCriteriasToString() {
			return subCriterias.toString();
		}

		public List<SubCriteria> getSubCriterias() {
			return subCriterias;
		}

		public int getOrder() {
			int order = mainCriteria.charAt(0) * 10000;
			if (mainCriteria.length() > 1) {
				order += mainCriteria.charAt(1);
			}
			return order;
		}
	}

	public static class SubCriteria {

		private final List<String> specifications = new ArrayList<>();
		private final char subCriteria;
		private final int order;

		public SubCriteria(char subCriteria, int order) {
			this.subCriteria = subCriteria;
			this.order = order;
		}

		public void addSpecification(String specification) {
			this.specifications.add(specification);
		}

		public void addSpecifications(String ... specifications) {
			for (String s : specifications) {
				this.specifications.add(s);
			}
		}

		public char getSubCriteria() {
			return subCriteria;
		}

		public boolean hasSpecifications() {
			return !specifications.isEmpty();
		}

		public List<String> getSpecifications() {
			return specifications;
		}

		@Override
		public String toString() {
			return String.valueOf(subCriteria);
		}

		public boolean hasSpecification(String givenSpecification) {
			return specifications.contains(givenSpecification);
		}

		public int getOrder() {
			return order;
		}
	}

	public static CriteriaFormatValidator forCriteria(String criteria) {
		if (!VALIDATORS.containsKey(criteria)) throw new IllegalArgumentException("Unknown criteria " + criteria);
		return VALIDATORS.get(criteria);
	}

	private static CriteriaFormatValidator forCriteria(MainCriteria mainCriteria) {
		String c = String.valueOf(mainCriteria.getMainCriteria().charAt(0));
		try {
			return forCriteria(c);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private final Map<String, MainCriteria> mainCriterias = new LinkedHashMap<>();

	private MainCriteria getMainCriteria(String mainCriteriaPrefix) {
		return mainCriterias.get(mainCriteriaPrefix);
	}

	private boolean hasMainCriteria(String mainCriteriaPrefix) {
		return mainCriterias.containsKey(mainCriteriaPrefix);
	}

	private MainCriteria addMainCriteria(String mainCriteria) {
		MainCriteria m = new MainCriteria(mainCriteria);
		this.mainCriterias.put(mainCriteria, m);
		return m;
	}

	public static String toCriteriaString(List<MainCriteria> criterias) {
		StringBuilder b = new StringBuilder();
		String prevPrefix = null;
		for (MainCriteria mainCriteria : criterias) {
			if (!given(mainCriteria.getMainCriteria())) continue;
			if (prevPrefix == null || !mainCriteria.getMainCriteria().startsWith(prevPrefix)) {
				if (prevPrefix != null) {
					b.append("; ");
				}
				b.append(mainCriteria.getMainCriteria());
			} else {
				b.append("+");
				b.append(mainCriteria.getMainCriteria().substring(1, mainCriteria.getMainCriteria().length()));
			}
			prevPrefix = String.valueOf(mainCriteria.getMainCriteria().charAt(0));
			for (SubCriteria subCriteria : mainCriteria.getSubCriterias()) {
				b.append(subCriteria.getSubCriteria());
				if (subCriteria.hasSpecifications()) {
					b.append("(");
					Iterator<String> i = subCriteria.getSpecifications().iterator();
					while (i.hasNext()) {
						b.append(i.next());
						if (i.hasNext()) {
							b.append(",");
						}
					}
					b.append(")");
				}
			}
		}
		return b.toString();
	}

	private static boolean given(String s) {
		return s != null && s.length() > 0;
	}

	public static List<MainCriteria> parseCriteria(String criteria) {
		try {
			if (criteria == null) return Collections.emptyList();
			criteria = Utils.removeWhitespace(criteria);
			if (criteria.length() < 1) return Collections.emptyList();
			if (!Character.isUpperCase(criteria.charAt(0))) return Collections.emptyList();

			List<MainCriteria> mainCriterias = new ArrayList<>();
			if (criteria.contains(";")) {
				for (String part : criteria.split(Pattern.quote(";"))) {
					mainCriterias.addAll(parseCriteriaGroup(part));
				}
			} else {
				mainCriterias.addAll(parseCriteriaGroup(criteria));
			}
			return mainCriterias;
		} catch (Exception e) {
			throw new RuntimeException("Parsing criteria failed: '" + criteria + "'.", e);
		}
	}

	private static Collection<MainCriteria> parseCriteriaGroup(String criteria) {
		List<MainCriteria> mainCriterias = new ArrayList<>();
		if (criteria.length() < 1) return mainCriterias;
		char group = criteria.charAt(0);
		if (criteria.contains("+")) {
			for (String part : criteria.split(Pattern.quote("+"))) {
				mainCriterias.add(parseCriteriaSingle(group, part));
			}
		} else {
			mainCriterias.add(parseCriteriaSingle(group, criteria));
		}
		return mainCriterias;
	}

	private static MainCriteria parseCriteriaSingle(char group, String criteria) {
		if (!criteria.startsWith(String.valueOf(group))) {
			criteria = group + criteria;
		}
		String mainPrefix = parseMainCriteriaPrefix(criteria);
		MainCriteria mainCriteria = new MainCriteria(mainPrefix);
		String remaining = criteria.substring(mainPrefix.length(), criteria.length());
		if (remaining.length() > 0) {
			parseSubCriterias(mainCriteria, remaining);
		}
		return mainCriteria;
	}

	private static void parseSubCriterias(MainCriteria mainCriteria, String subCriteriaPart) {
		// abc(ii)d(i,iv) 
		boolean specificationsOpen = false;
		SubCriteria subCriteria = null;
		String specification = "";
		int order = 1;
		for (char c : subCriteriaPart.toCharArray()) {
			if (c == '(') {
				specificationsOpen = true;
				continue;
			}
			if (c == ')') {
				specificationsOpen = false;
				if (subCriteria == null) continue; // stray end tag
				if (specification.length() > 0) {
					subCriteria.addSpecification(specification);
				}
				mainCriteria.addSubCriteria(subCriteria);
				specification = "";
				subCriteria = null;
				continue;
			} 
			if (specificationsOpen) {
				if (c == ',') {
					if (specification.length() > 0) {
						subCriteria.addSpecification(specification);
					}
					specification = "";
					continue;
				}
				specification += c;
				continue;
			}
			if (subCriteria != null) {
				mainCriteria.addSubCriteria(subCriteria);
			}
			subCriteria = new SubCriteria(c, order++);
		}
		if (subCriteria != null) {
			mainCriteria.addSubCriteria(subCriteria);
		}
	}

	private static String parseMainCriteriaPrefix(String criteria) {
		String mainGroupPrefix = "";
		for (char c : criteria.toCharArray()) {
			if (Character.isUpperCase(c) || Character.isDigit(c)) {
				mainGroupPrefix += c;
			} else {
				break;
			}
		}
		return mainGroupPrefix;
	}

	public CriteriaValidationResult validate(String criteria) {
		if (criteria == null) return new CriteriaValidationResult("Ohjelmointivirhe: null criteria");
		if (!Utils.removeWhitespace(criteria).equals(criteria)) return new CriteriaValidationResult("Ohjelmointivirhe: ei ole poistettu whitespacea");
		List<MainCriteria> criterias = parseCriteria(criteria);
		String formattedCriteria = toCriteriaString(criterias);
		if (!formattedCriteria.equals(criteria)) return new CriteriaValidationResult("Kriteeri on väärin muotoiltu. Tarkista \"+\"-merkin, sulkujen ja pilkun käyttö. Annettu: "+criteria+", pitäisi olla "+formattedCriteria);
		if (criterias.isEmpty()) return VALID;

		MainCriteria prev = null; 
		for (MainCriteria mainCriteria : criterias) {
			if (prev != null && mainCriteria.getOrder() < prev.getOrder()) {
				return new CriteriaValidationResult("Kriteeri " + mainCriteria.getMainCriteria() + " tulisi ilmoittaa ennen kriteeriä " + prev.getMainCriteria());
			}
			CriteriaValidationResult result = validate(mainCriteria);
			if (!result.isValid()) return result;
			prev = mainCriteria;
		}
		return VALID;
	}

	private CriteriaValidationResult validate(MainCriteria given) {
		if (!this.hasMainCriteria(given.getMainCriteria())) {
			return new CriteriaValidationResult("Tuntematon kriteeri " + given.getMainCriteria());
		}
		MainCriteria specifiecMainCriteria = this.getMainCriteria(given.getMainCriteria());
		if (specifiecMainCriteria.hasSubCriterias() && !given.hasSubCriterias()) {
			return new CriteriaValidationResult("Kriteerille " + specifiecMainCriteria.getMainCriteria() + " täytyy antaa yksi alakriteereistä " + specifiecMainCriteria.subCriteriasToString());
		}
		SubCriteria prevSubCriteria = null;
		for (SubCriteria givenSubCriteria : given.getSubCriterias()) {
			if (prevSubCriteria != null && givenSubCriteria.getSubCriteria() == prevSubCriteria.getSubCriteria()) {
				return new CriteriaValidationResult("Kriteerille " + specifiecMainCriteria.getMainCriteria() + " on annettu alakriteeri " + givenSubCriteria.getSubCriteria() + " useammin kuin kerran");
			}
			SubCriteria specifiedSubCriteria = specifiecMainCriteria.getSubCriteria(givenSubCriteria.getSubCriteria());
			if (specifiedSubCriteria == null) {
				return new CriteriaValidationResult("Kriteerille " + specifiecMainCriteria.getMainCriteria() + " ei ole määritelty alakriteeriä " + givenSubCriteria.getSubCriteria());
			}
			if (prevSubCriteria != null && specifiedSubCriteria.getOrder() < prevSubCriteria.getOrder() ) {
				return new CriteriaValidationResult("Kriteerin " + specifiecMainCriteria.getMainCriteria() + " alakriteerin " + givenSubCriteria.getSubCriteria() + " täytyy olla ennen alakriteeriä " + prevSubCriteria.getSubCriteria());
			}
			if (specifiedSubCriteria.hasSpecifications() && !givenSubCriteria.hasSpecifications()) {
				return new CriteriaValidationResult("Kriteerille " + specifiecMainCriteria.getMainCriteria() + specifiedSubCriteria.getSubCriteria() + " täytyy antaa vähintään yksi lisämerkinnöistä " + specifiedSubCriteria.getSpecifications());
			}
			String prevSpecification = null;
			for (String givenSpecification : givenSubCriteria.getSpecifications()) {
				if (givenSpecification.equals(prevSpecification)) {
					return new CriteriaValidationResult("Kriteerille " + specifiecMainCriteria.getMainCriteria() + specifiedSubCriteria.getSubCriteria() + " on annettu lisämerkintä " + prevSpecification + " useammin kuin kerran");
				}
				if (!specifiedSubCriteria.hasSpecification(givenSpecification)) {
					return new CriteriaValidationResult("Kriteerille " + specifiecMainCriteria.getMainCriteria() + specifiedSubCriteria.getSubCriteria() + " ei ole määritelty lisämerkintää " + givenSpecification);
				}
				if (prevSpecification != null && specificationOrder(givenSpecification) < specificationOrder(prevSpecification)) {
					return new CriteriaValidationResult("Kriteerin " + specifiecMainCriteria.getMainCriteria() + specifiedSubCriteria.getSubCriteria() + " lisämerkinnän " + givenSpecification + " tulis olla ennen lisämerkintää " + prevSpecification);
				}
				prevSpecification = givenSpecification;
			}
			prevSubCriteria = specifiedSubCriteria;
		}
		return VALID;
	}

	private static final Map<String, Integer> SPECIFICATION_ORDERS;
	static {
		SPECIFICATION_ORDERS = new HashMap<>();
		SPECIFICATION_ORDERS.put("i", 1);
		SPECIFICATION_ORDERS.put("ii", 2);
		SPECIFICATION_ORDERS.put("iii", 3);
		SPECIFICATION_ORDERS.put("iv", 4);
		SPECIFICATION_ORDERS.put("v", 5);
		SPECIFICATION_ORDERS.put("vi", 6);
	}

	private int specificationOrder(String specification) {
		return SPECIFICATION_ORDERS.get(specification);
	}

	public static CriteriaValidationResult validateJoined(String criteria) {
		if (criteria == null) return new CriteriaValidationResult("Ohjelmointivirhe: null criteria");
		if (!criteria.trim().equals(criteria)) return new CriteriaValidationResult("Ohjelmointivirhe: ei ole trimmattu");
		List<MainCriteria> criterias = parseCriteria(criteria);
		String formattedCriteria = toCriteriaString(criterias);
		if (!formattedCriteria.equals(criteria)) return new CriteriaValidationResult("Kriteeri on väärin muotoiltu. Tarkista \"+\"-merkin, sulkujen ja pilkun käyttö. Annettu: "+criteria+", pitäisi olla "+formattedCriteria);
		if (criterias.isEmpty()) return VALID;

		MainCriteria prev = null; 
		for (MainCriteria mainCriteria : criterias) {
			if (prev != null && mainCriteria.getOrder() < prev.getOrder()) {
				return new CriteriaValidationResult("Kriteeri " + mainCriteria.getMainCriteria() + " tulisi ilmoittaa ennen kriteeriä " + prev.getMainCriteria());
			}
			CriteriaFormatValidator validator = CriteriaFormatValidator.forCriteria(mainCriteria);
			if (validator == null) return new CriteriaValidationResult("Tuntematon kriteeri " + mainCriteria.getMainCriteria());
			CriteriaValidationResult result = validator.validate(mainCriteria);
			if (!result.isValid()) {
				return result;
			}
			prev = mainCriteria;
		}
		return VALID;
	}

}
