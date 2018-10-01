package fi.luomus.triplestore.taxonomy.iucn.runnable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNHabitatObject;

public class IUCNLineData {

	public enum Mode {
		V2010, SAMMALLEET2019, V2000, GLOBAL, LUOKKA_MUUTOSSYY

	}
	private static final Qname OCC_EX = new Qname("MX.typeOfOccurrenceExtirpated");
	private static final Qname DOES_NOT_OCCUR = new Qname("MX.doesNotOccur");
	private static final Qname OCCURS = new Qname("MX.typeOfOccurrenceOccurs");
	private static final Qname OCC_ANTROPOGENIC = new Qname("MX.typeOfOccurrenceAnthropogenic");
	private static final Qname OCC_UNCERTAIN = new Qname("MX.typeOfOccurrenceUncertain");
	private static final Qname RARE = new Qname("MX.typeOfOccurrenceRareVagrant");
	private static final Qname NOT_ESTAMBLISHED = new Qname("MX.typeOfOccurrenceNotEstablished");
	private static final Qname ANTHROPOGENIC = OCC_ANTROPOGENIC;
	private static final Qname EXTIRPATED = OCC_EX;
	private static final Qname STABLE = new Qname("MX.typeOfOccurrenceStablePopulation");
	public String taxonQname;
	public String scientificName;
	public List<String> synonyms = new ArrayList<>();
	public String finnishName;
	public String alternativeFinnishNames;
	public String taxonomicNotes;
	public String typeOfOccurrenceInFinland;
	public String distributionArea;
	public String occurrenceArea;
	public Map<Qname, String> occurences = new LinkedHashMap<>();
	public String primaryHabitat;
	public String secondaryHabitats;
	public String habitatNotes;
	public String occurrenceNotes;
	public String generationAge;
	public String evaluationPeriodLength;
	public String individualCount;
	public String individualCountNotes;
	public String populationSizePeriodBeginning;
	public String populationSizePeriodEnd;
	public String decreaseDuringPeriod;
	public String populationVaries;
	public String populationVariesNotes;
	public String fragmentedHabitats;
	public String fragmentedHabitatsNotes;
	public String borderGain;
	public String borderGainNotes;
	public String endangermentReasons;
	public String threats;
	public String criteriaA;
	public String criteriaB;
	public String criteriaC;
	public String criteriaD;
	public String criteriaE;
	public String criteriaANotes;
	public String criteriaBNotes;
	public String criteriaCNotes;
	public String criteriaDNotes;
	public String criteriaENotes;
	public String groundsForEvaluationNotes;
	public String redListStatus;
	public String criteriaForStatus;
	public String exteralPopulationImpactOnRedListStatus;
	public String reasonForStatusChange;
	public String redListStatusRange;
	public String possiblyRE;
	public String lastSightingNotes;
	public String lsaRecommendation;
	public String lsaRecommendationNotes;
	public String legacyPublications;
	public String redListStatusAccuracyNotes;
	public String editNotes;
	public String legacyInformalGroup;
	private final String[] parts;

	public IUCNLineData(Mode mode, String[] parts) {
		this.parts = parts;
		if (mode == Mode.V2010) {
			v2010();
		} else if (mode == Mode.SAMMALLEET2019) {
			sammaleet2019();
		} else if (mode == Mode.V2000) {
			v2000();
		} else if (mode == Mode.GLOBAL) {
			global();
		} else if (mode == Mode.LUOKKA_MUUTOSSYY) {
			luokkaMuutoksensyy();
		} else {
			throw new UnsupportedOperationException(""+ mode);
		}
	}

	private void luokkaMuutoksensyy() {
		taxonQname = s(0);
		redListStatus = s(1);
		reasonForStatusChange = s(2);
	}

	private void global() {
		String genus = s(6);
		String species = s(7);
		String infrarank = s(9);
		String infraspec = s(10);
		scientificName = sciname(genus, species, infrarank, infraspec);
		for (String synonym : parseSynonyms(s(13))) {
			synonyms.add(synonym);
		}
		redListStatus = s(17);
		criteriaForStatus = s(18);
		legacyInformalGroup = s(2);
	}

	private List<String> parseSynonyms(String s) {
		if (!given(s)) return Collections.emptyList();
		List<String> synonyms = new ArrayList<>();
		if (s.contains(";")) {
			for (String part : s.split(Pattern.quote(";"))) {
				part = part.trim();
				if (given(part)) {
					synonyms.add(part);
				}
			}
		} else {
			synonyms.add(s);
		}
		return synonyms;
	}

	private String sciname(String genus, String species, String infrarank, String infraspec) {
		String sciname = genus + " " + species;
		if (given(infrarank)) sciname += " " + infrarank;
		if (given(infraspec)) sciname += " " + infraspec;
		sciname = sciname.trim();
		while (sciname.contains("  ")) {
			sciname = sciname.replace("  ", " ");
		}
		return sciname;
	}

	public IUCNLineData(String[] parts) {
		this(Mode.V2010, parts);		
	}

	private void v2010() {
		String sciNameField = s(1);
		if (sciNameField.startsWith("MX.")) {
			taxonQname = sciNameField;
		} else {
			scientificName = sciNameField;
		}
		legacyInformalGroup = s(9);
		finnishName = s(12);
		alternativeFinnishNames = s(13);
		taxonomicNotes = s(16);
		typeOfOccurrenceInFinland = s(17);
		distributionArea = s(18);
		occurrenceArea = s(19);
		occurences.put(new Qname("ML.690"), s(20));
		occurences.put(new Qname("ML.691"), s(21));
		occurences.put(new Qname("ML.692"), s(22));
		occurences.put(new Qname("ML.693"), s(23));
		occurences.put(new Qname("ML.694"), s(24));
		occurences.put(new Qname("ML.695"), s(25));
		occurences.put(new Qname("ML.696"), s(26));
		occurences.put(new Qname("ML.697"), s(27));
		occurences.put(new Qname("ML.698"), s(28));
		occurences.put(new Qname("ML.699"), s(29));
		occurences.put(new Qname("ML.700"), s(30));
		primaryHabitat = s(31);
		secondaryHabitats = s(32);
		habitatNotes = s(33);
		occurrenceNotes = s(34);
		generationAge = s(35);
		evaluationPeriodLength = s(36);
		individualCount = s(37);
		populationSizePeriodBeginning = s(39);
		populationSizePeriodEnd = s(40);
		decreaseDuringPeriod = s(41);
		populationVaries = s(42);
		fragmentedHabitats = s(43);
		borderGain = s(44);
		endangermentReasons = s(45);
		threats = s(46);
		criteriaA = s(47);
		criteriaB = s(48);
		criteriaC = s(49);
		criteriaD = s(50);
		criteriaE = s(51);
		groundsForEvaluationNotes = s(52);
		redListStatus = s(55);
		criteriaForStatus = s(56);
		reasonForStatusChange = s(57);
		redListStatusRange = s(58);
		possiblyRE = s(61);
		lastSightingNotes = s(62);
		lsaRecommendation = s(65);
		legacyPublications = s(68);
	}

	private void v2000() {
		String sciNameField = s(2);
		if (sciNameField.startsWith("MX.")) {
			taxonQname = sciNameField;
		} else {
			scientificName = sciNameField;
		}
		primaryHabitat = s(6);
		endangermentReasons = s(7);
		threats = s(8);
		redListStatus = s(3);
		criteriaForStatus = s(4);
		exteralPopulationImpactOnRedListStatus = s(5);
		legacyInformalGroup = s(1);
	}

	private void sammaleet2019() {
		taxonQname = s(0);
		taxonomicNotes = s(1);
		typeOfOccurrenceInFinland = s(2);
		distributionArea = s(3);
		occurrenceArea = s(4);
		occurences.put(new Qname("ML.690"), s(5));
		occurences.put(new Qname("ML.691"), s(6));
		occurences.put(new Qname("ML.692"), s(7));
		occurences.put(new Qname("ML.693"), s(8));
		occurences.put(new Qname("ML.694"), s(9));
		occurences.put(new Qname("ML.695"), s(10));
		occurences.put(new Qname("ML.696"), s(11));
		occurences.put(new Qname("ML.697"), s(12));
		occurences.put(new Qname("ML.698"), s(13));
		occurences.put(new Qname("ML.699"), s(14));
		occurences.put(new Qname("ML.700"), s(15));
		primaryHabitat = s(16);
		secondaryHabitats = s(17);
		habitatNotes = s(18);
		occurrenceNotes = s(19);
		generationAge = s(20);
		evaluationPeriodLength = s(21);
		individualCount = s(23);
		individualCountNotes = s(22);
		populationSizePeriodBeginning = s(24);
		populationSizePeriodEnd = s(25);
		decreaseDuringPeriod = s(26);
		populationVaries = s(28);
		populationVariesNotes = s(27);
		fragmentedHabitats = s(30);
		fragmentedHabitatsNotes = s(29);

		borderGain = s(32);
		borderGainNotes = s(31);

		endangermentReasons = s(33);
		threats = s(35);

		criteriaA = s(37);
		criteriaANotes = s(38);
		criteriaB = s(39);
		criteriaBNotes = s(40);
		criteriaC = s(41);
		criteriaCNotes = s(42);
		criteriaD = s(43);
		criteriaDNotes = s(44);

		groundsForEvaluationNotes = s(45);
		redListStatus = s(46);
		criteriaForStatus = s(47);
		reasonForStatusChange = s(50);
		redListStatusRange = s(48);
		possiblyRE = s(51);
		lastSightingNotes = s(52);
		lsaRecommendation = s(54);
		lsaRecommendationNotes = s(55);
		legacyPublications = s(56);
		redListStatusAccuracyNotes = s(53);
		editNotes = s(57);
	}

	private String s(int i) {
		try {
			String s = parts[i].trim();
			while (s.contains("  ")) {
				s = s.replace("  ", " ");
			}
			return s.trim();
		} catch (ArrayIndexOutOfBoundsException e) {
			return "";
		}
	}

	public String getScientificName() {
		return scientificName;
	}

	public List<String> getSynonyms() {
		return synonyms;
	}

	public String getFinnishName() {
		return finnishName;
	}

	public List<String> getAlternativeFinnishNames() {
		if (!given(alternativeFinnishNames)) return Collections.emptyList();
		List<String> alternative = new ArrayList<>();
		for (String  s : alternativeFinnishNames.split(Pattern.quote(","))) {
			alternative.add(s.trim());
		}
		return alternative;
	}

	private boolean given(String s) {
		return s != null && s.trim().length() > 0;
	}

	public String getTaxonomicNotes() {
		return taxonomicNotes;
	}

	public Qname getTypeOfOccurrenceInFinland() {
		if (!given(typeOfOccurrenceInFinland)) return null;
		String s = typeOfOccurrenceInFinland.toLowerCase().replace("<", "").replace(">", "").replace("n.", "").trim();
		if (s.equals("todennäköisesti vakinainen")) return null;
		if (s.contains("mahdollisesti") || s.contains("osin")) return null;
		if (s.contains(" x ")) return null;
		if (validInteger(s)) return STABLE;
		if (!s.contains("?") && (s.contains("tilapäisviipyjä") || s.contains("tilapäinen") || s.contains("levähtävä"))) return RARE;
		if (s.contains("satunnainen")) return RARE;
		if (!s.contains("?") && s.contains("hävinnyt")) return EXTIRPATED;
		if (s.contains("vieraslaji") || s.contains("import") || s.contains("koriste") || s.contains("sisätil") || s.contains("indoor") || s.contains("vilja") || s.contains("viljely") || s.contains("vihannes") || s.contains("marjapens")) return ANTHROPOGENIC;
		if (!s.contains("?") && (s.contains("vakituinen") ||  s.contains("vakinainen") || s.contains("vakiintunut"))) return STABLE;
		if (s.contains("lisääntyvä") || s.contains("alkuperäinen") || s.equals("muinaistulokas")) return STABLE;
		if (s.contains("uusi laji") || s.contains("uutena") || s.contains("uustulo")) return NOT_ESTAMBLISHED;
		return null;
	}

	private boolean validInteger(String s) {
		if (!given(s)) return false;
		try {
			Integer.valueOf(s);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public String getTypeOfOccurrenceInFinlandNotes() {
		return typeOfOccurrenceInFinland;
	}

	public Integer getDistributionAreaMin() {
		return getMin(distributionArea);
	}

	public Integer getDistributionAreaMax() {
		return getMax(distributionArea);
	}

	public String getDistributionAreaNotes() {
		return distributionArea;
	}

	public Integer getOccurrenceAreaMin() {
		return getMin(occurrenceArea);
	}

	public Integer getOccurrenceAreaMax() {
		return getMax(occurrenceArea);
	}

	public String getOccurrenceAreaNotes() {
		return occurrenceArea;
	}

	public Map<Qname, Qname> getOccurrences() {
		if (occurences.isEmpty()) return Collections.emptyMap();
		Map<Qname, Qname> interpeted = new HashMap<>();
		for (Map.Entry<Qname, String> e : occurences.entrySet()) {
			Qname status = getStatus(e.getValue());
			if (status != null) {
				interpeted.put(e.getKey(), status);
			}
		}
		return interpeted;
	}

	private Qname getStatus(String s) {
		s = s.toLowerCase();
		if (s.equals("hävinnyt")) return OCC_EX;
		if (s.equals("esiintyy")) return OCCURS;
		if (s.equals("ei havaintoja")) return DOES_NOT_OCCUR;
		if (s.equals("satunnainen")) return RARE;
		if (s.equals("esiintyy mahdollisesti")) return OCC_UNCERTAIN;
		if (s.contains("?") || s.contains("(")) return OCC_UNCERTAIN;
		if (s.equals("na")) return OCC_ANTROPOGENIC; 
		if (s.equals("x") || s.equals("+") || s.contains("rt")) return OCCURS;
		if (s.equals("-") || s.equals("_") || s.equals("­")) return DOES_NOT_OCCUR;
		if (s.equals("re")) return OCC_EX;
		return null;
	}

	public String getOccurrenceRegionsNotes() {
		if (occurences.isEmpty()) return "";
		StringBuilder b = new StringBuilder();
		Iterator<Map.Entry<Qname, String>> i = occurences.entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry<Qname, String> e = i.next();
			String area = getAreaAbbrv(e.getKey());
			b.append(area).append(": ").append(e.getValue());
			if (i.hasNext()) b.append(", ");
		}
		return b.toString();
	}

	private static final Map<Qname, String> AREAS;
	static {
		AREAS = new HashMap<>();
		AREAS.put(new Qname("ML.690"), "1a");
		AREAS.put(new Qname("ML.691"), "1b");
		AREAS.put(new Qname("ML.692"), "2a");
		AREAS.put(new Qname("ML.693"), "2b");
		AREAS.put(new Qname("ML.694"), "3a");
		AREAS.put(new Qname("ML.695"), "3b");
		AREAS.put(new Qname("ML.696"), "3c");
		AREAS.put(new Qname("ML.697"), "4a");
		AREAS.put(new Qname("ML.698"), "4b");
		AREAS.put(new Qname("ML.699"), "4c");
		AREAS.put(new Qname("ML.700"), "4d");
	}

	private String getAreaAbbrv(Qname id) {
		return AREAS.get(id);
	}

	public IUCNHabitatObject getPrimaryHabitat() {
		if (!given(primaryHabitat)) return null;
		List<IUCNHabitatObject> list = getHabitats(primaryHabitat);
		if (list.isEmpty()) return null;
		return list.get(0);
	}

	public List<IUCNHabitatObject> getSecondaryHabitats() {
		if (!given(secondaryHabitats)) return Collections.emptyList();
		List<IUCNHabitatObject> list = new ArrayList<>();
		List<IUCNHabitatObject> primary = getHabitats(primaryHabitat);
		if (primary.size() > 1) {
			Iterator<IUCNHabitatObject> i = primary.iterator();
			i.next();
			while (i.hasNext()) {
				IUCNHabitatObject o = i.next();
				if (o != null) list.add(o);
			}
		}
		if (given(secondaryHabitats)) {
			for (IUCNHabitatObject o : getHabitats(secondaryHabitats)) {
				if (o != null) list.add(o);
			}
		}
		return list;
	}

	private List<IUCNHabitatObject> getHabitats(String habitats) {
		List<IUCNHabitatObject> list = new ArrayList<>();
		habitats = Utils.removeWhitespace(habitats).replace("(", "").replace(")", "").replace("?", "").replace("*", "");
		Set<String> unique = new LinkedHashSet<>();
		for (String s : habitats.split(Pattern.quote(","))) {
			unique.add(s);
		}
		int i = 1;
		for (String s : unique) {
			Qname habitatId = HABITAS.get(s);
			if (habitatId != null) {
				list.add(new IUCNHabitatObject(null, habitatId, i++));
				continue;
			}
			Object[] o = getSpecificTypesFromEnd(s);
			String remainingHabitat = (String) o[0];
			@SuppressWarnings("unchecked")
			Set<Qname> specificTypes = (Set<Qname>) o[1];
			if (!specificTypes.isEmpty()) {
				habitatId = HABITAS.get(remainingHabitat);
				if (habitatId == null) continue; // There was a valid habitat specific type but no mapping for habitat
				IUCNHabitatObject habitatObject = new IUCNHabitatObject(null, habitatId, i++);
				for (Qname specificType : specificTypes) {
					habitatObject.addHabitatSpecificType(specificType);
				}
				list.add(habitatObject);
				continue;
			}
			list.add(null);
		}
		return list;
	}

	private Object[] getSpecificTypesFromEnd(String s) {
		Set<Qname> types = new HashSet<>();
		int prevSize = 0;
		while (true) {
			if (HABITAS.containsKey(s)) break; // shortened string is now a valid habitat
			for (String type : HABITAT_SPECIFIC_TYPES.keySet()) {
				if (s.endsWith(type)) {
					types.add(HABITAT_SPECIFIC_TYPES.get(type));
					s = s.substring(0, s.length() - type.length());
					break;
				}
			}
			if (prevSize == types.size()) {
				break; // nothing new found
			}
			prevSize = types.size();
		}
		return new Object[] { s, types};
	}

	public String getHabitatNotes() {
		StringBuilder b = new StringBuilder();
		if (given(primaryHabitat)) b.append("Ensisijainen: " + primaryHabitat);
		if (given(secondaryHabitats)) b.append("; Muut: ").append(secondaryHabitats);
		return b.toString();
	}

	public String getHabitatGeneralNotes() {
		return habitatNotes;
	}

	public String getOccurrenceNotes() {
		return occurrenceNotes;
	}

	public Double getGenerationAge() {
		if (!given(generationAge)) return null;
		if (generationAge.contains("tai")) return null;
		List<String> parts = new ArrayList<>();
		for (String s : generationAge.split(Pattern.quote("("))[0].split(Pattern.quote("-"))) {
			s = removeNonDigits(s.replace(",", "."));
			if (given(s)) {
				parts.add(s);				
			}
		}
		if (parts.isEmpty()) return null;
		if (parts.size() == 1) {
			try {
				return Double.valueOf(parts.get(0));
			} catch (Exception e) {
				return null;
			}
		}

		try {
			Double v1 = Double.valueOf(parts.get(0));
			Double v2 = Double.valueOf(parts.get(1));
			return Utils.avg(v1, v2);
		} catch (Exception e) {
			return null;
		}
	}

	private String removeNonDigits(String s) {
		StringBuilder b = new StringBuilder();
		for (char c : s.toCharArray()) {
			if (c == '.' || Character.isDigit(c)) {
				b.append(c);
			}
		}
		return b.toString();
	}

	public String getGenerationAgeNotes() {
		if (!given(generationAge)) return "";
		Double interpeted = getGenerationAge();
		if (interpeted == null) return generationAge;
		if (interpeted.toString().equals(generationAge)) return "";
		if (interpeted.toString().equals(generationAge.replace(",", "."))) return "";
		if (String.valueOf(interpeted.intValue()).equals(generationAge)) return "";
		return generationAge;
	}

	public Integer getEvaluationPeriodLength() {
		if (!given(evaluationPeriodLength)) return null;
		try {

			String s = evaluationPeriodLength.split(Pattern.quote("("))[0].replace("/", "-").split(Pattern.quote("-"))[0].trim();
			s = s.replace(",", ".");
			s = removeNonDigits(s);
			if (s.startsWith(".")) s = s.substring(1, s.length());
			if (!given(s)) return null;
			return (int) Math.floor(Double.valueOf(s));
		} catch (Exception e) {
			return null;
		}
	}

	public String getEvaluationPeriodLengthNotes() {
		return evaluationPeriodLength;
	}

	public Integer getIndividualCountMin() {
		return getMin(individualCount);
	}

	public Integer getIndividualCountMax() {
		return getMax(individualCount);
	}

	public String getIndividualCountNotes() {
		if (individualCountNotes != null) return individualCountNotes;
		return individualCount;
	}

	public String getPopulationSizePeriodNotes() {
		StringBuilder b = new StringBuilder();
		if (given(populationSizePeriodBeginning)) {
			b.append("Alussa: ").append(populationSizePeriodBeginning);
		}
		if (given(populationSizePeriodEnd)) {
			b.append(" Lopussa: ").append(populationSizePeriodEnd);
		}
		return b.toString().trim();
	}

	public Integer getPopulationSizePeriodBeginning() {
		String s = cleanMinmax(populationSizePeriodBeginning);
		if (given(s)) return iVal(s);
		return null;
	}

	public Integer getPopulationSizePeriodEnd() {
		String s = cleanMinmax(populationSizePeriodEnd);
		if (given(s)) return iVal(s);
		return null;
	}

	public String getDecreaseDuringPeriodNotes() {
		return decreaseDuringPeriod;
	}

	public Boolean getPopulationVaries() {
		return bVal(populationVaries);
	}

	private Boolean bVal(String s) {
		if (!given(s)) return null;
		s = s.replace("?", "").toLowerCase();
		if (!given(s)) return null;
		if (s.equals("kyllä") || s.equals("x") || s.equals("on") || s.equals("+") || s.equals("(x)")) return true;
		if (s.equals("ei")) return false;
		if (s.contains("voimakas")) return true;
		if (s.equals("selvä")) return true;
		return null;
	}

	public String getPopulationVariesNotes() {
		if (populationVariesNotes != null) return populationVariesNotes;
		return populationVaries;
	}

	public Boolean getFragmentedHabitats() {
		return bVal(fragmentedHabitats);
	}

	public String getFragmentedHabitatsNotes() {
		if (fragmentedHabitatsNotes != null) return fragmentedHabitatsNotes;
		return fragmentedHabitats;
	}

	public Boolean getBorderGain() {
		return bVal(borderGain);
	}

	public String getBorderGainNotes() {
		if (borderGainNotes != null) return borderGainNotes;
		return borderGain;
	}

	public List<Qname> getEndangermentReasons() {
		return reasons(endangermentReasons);
	}

	private List<Qname> reasons(String s) {
		if (!given(s)) return Collections.emptyList();
		s = Utils.removeWhitespace(s);
		List<Qname> list = new ArrayList<>();
		for (String part : s.split(Pattern.quote(","))) {
			Qname r = ENDANGERMENT_REASONS.get(part);
			if (r != null) list.add(r);
		}
		return list;
	}

	public List<Qname> getThreats() {
		return reasons(threats);
	}

	private String criteria(String criteria) {
		if (criteria == null) return null;
		if (criteria.contains(":")) {
			return criteria.split(Pattern.quote(":"))[0];
		}
		return criteria;
	}

	public Qname status(String criteria) {
		if (criteria == null) return null;
		if (!criteria.contains(":")) return null;
		String status = criteria.split(Pattern.quote(":"))[1].trim();
		return RED_LIST_STATUSES.get(status);
	}

	public String getCriteriaA() { return criteria(criteriaA); }
	public String getCriteriaB() { return criteria(criteriaB); }
	public String getCriteriaC() { return criteria(criteriaC); }
	public String getCriteriaD() { return criteria(criteriaD); }
	public String getCriteriaE() { return criteria(criteriaE); }
	public Qname getCriteriaAStatus() { return status(criteriaA); }
	public Qname getCriteriaBStatus() { return status(criteriaB); }
	public Qname getCriteriaCStatus() { return status(criteriaC); }
	public Qname getCriteriaDStatus() { return status(criteriaD); }
	public Qname getCriteriaEStatus() { return status(criteriaE); }

	public String getGroundsForEvaluationNotes() { return groundsForEvaluationNotes; }
	public String getCriteriaForStatus() {return criteriaForStatus; }

	public List<Qname> getReasonForStatusChange() {
		if (!given(reasonForStatusChange)) return Collections.emptyList();
		String s = Utils.removeWhitespace(reasonForStatusChange);
		List<Qname> list = new ArrayList<>();
		for (String part : s.split(Pattern.quote(","))) {
			Qname r = STATUS_CHANGE_REASONS.get(part.toLowerCase());
			if (r != null) list.add(r);
		}
		return list;
	}

	public String getReasonForStatusChangeNotes() {
		return reasonForStatusChange;
	}

	public Qname getRedListStatus() {
		String s = redListStatus.replace(".", "").replace("*", "").replace("∙", "").trim().toUpperCase();
		if (s.contains("(")) {
			s = s.split(Pattern.quote("("))[0].trim();
		}
		return RED_LIST_STATUSES.get(s);
	}

	public String getRedListStatusNotes() {
		if (getRedListStatus() == null) return redListStatus;
		return "";
	}

	public Qname getRedListStatusMin() {
		if (redListStatusRange == null) return null;
		return RED_LIST_STATUSES.get(redListStatusRange.split("-")[0]);
	}

	public Qname getRedListStatusMax() {
		if (redListStatusRange == null) return null;
		if (!redListStatusRange.contains("-")) return getRedListStatusMin();
		return RED_LIST_STATUSES.get(redListStatusRange.split("-")[1]);
	}

	public Qname getPossiblyRE() {
		if (possiblyRE == null) return null;
		String s = possiblyRE.toLowerCase();
		if (s.equals("re") || s.equals("kyllä") || s.equals("x")) return new Qname("MX.iucnRE");
		if (validInteger(s)) return new Qname("MX.iucnRE");
		return null;
	}

	public String getPossiblyRENotes() {
		return possiblyRE;
	}

	public String getLastSightingNotes() {
		return lastSightingNotes;
	}

	public Boolean getLsaRecommendation() {
		if (lsaRecommendation == null) return null;
		String s = lsaRecommendation.toLowerCase();
		if (s.equals("e") || s.equals("e*")) return true;
		return null;
	};

	public String getLsaRecommendationNotes() {
		if (lsaRecommendationNotes != null) return lsaRecommendationNotes;
		return lsaRecommendation;
	};

	public String getLegacyPublications() {
		return legacyPublications;
	}

	private Integer getMin(String s) {
		s = cleanMinmax(s);
		if (s.contains("-")) {
			return iVal(s.split(Pattern.quote("-"))[0]);
		}
		if (s.startsWith(">")) return iVal(s.replace(">", ""));
		if (s.startsWith("<")) return null;
		return iVal(s);
	}

	private Integer getMax(String s) {
		s = cleanMinmax(s);
		if (s.endsWith("-")) return null;
		if (s.contains("-")) {
			return iVal(s.split(Pattern.quote("-"))[1]);
		}
		if (s.startsWith("<")) return iVal(s.replace("<", ""));
		return null;
	}

	private Integer iVal(String s) {
		try {
			return Integer.valueOf(s);
		} catch (Exception e) {
			return null;
		}
	}

	private String cleanMinmax(String s) {
		if (s == null) return "";
		return s.toLowerCase().replace("±2", "").replace("n.", "").replace("km²", "").replace("km2", "").replace(".", "").replace(" ", "").replace("noin", "").replace("arv.", "").replace("selvästi", "").replace("yli", ">").replace("?", "").replace("tunnettu", "").replace("alle","<").replace("ehkä", "").trim();
	}

	private static final Map<String, Qname> HABITAS;
	static {
		HABITAS = new HashMap<>();
		for (String s : "M,Mk,Mkk,Mkt,Ml,Mlt,Mlk,S,Sl,Sla,Slr,Slk,Sn,Snk,Snr,Sr,Srk,Srr,Sk,Skk,Skr,V,Vi,Vs,Vsk,Vsr,Va,Vj,Vp,Vk,Vl,R,Ri,Rih,Ris,Rit,Rj,Rjh,Rjs,Rjt,K,Kl,T,Tk,Tl,Tn,I,In,Ih,Io,Iv,Ip,Ir".split(",")) {
			HABITAS.put(s, new Qname("MKV.habitat"+s));
		}
	}

	private static final Map<String, Qname> HABITAT_SPECIFIC_TYPES;
	static {
		HABITAT_SPECIFIC_TYPES = new HashMap<>();
		HABITAT_SPECIFIC_TYPES.put("v", new Qname("MKV.habitatSpecificTypeV"));
		HABITAT_SPECIFIC_TYPES.put("h", new Qname("MKV.habitatSpecificTypeH"));
		HABITAT_SPECIFIC_TYPES.put("p", new Qname("MKV.habitatSpecificTypeP"));
		HABITAT_SPECIFIC_TYPES.put("pa", new Qname("MKV.habitatSpecificTypePAK"));
		HABITAT_SPECIFIC_TYPES.put("va", new Qname("MKV.habitatSpecificTypeVAK"));
	}

	private static final Map<String, Qname> ENDANGERMENT_REASONS;
	static {
		ENDANGERMENT_REASONS = new HashMap<>();
		ENDANGERMENT_REASONS.put("P", new Qname("MKV.endangermentReasonP"));
		ENDANGERMENT_REASONS.put("Ke", new Qname("MKV.endangermentReasonKe"));
		ENDANGERMENT_REASONS.put("H", new Qname("MKV.endangermentReasonH"));
		ENDANGERMENT_REASONS.put("Ku", new Qname("MKV.endangermentReasonKu"));
		ENDANGERMENT_REASONS.put("R", new Qname("MKV.endangermentReasonR"));
		ENDANGERMENT_REASONS.put("Ks", new Qname("MKV.endangermentReasonKs"));
		ENDANGERMENT_REASONS.put("Pm", new Qname("MKV.endangermentReasonPm"));
		ENDANGERMENT_REASONS.put("Pr", new Qname("MKV.endangermentReasonPr"));
		ENDANGERMENT_REASONS.put("N", new Qname("MKV.endangermentReasonN"));
		ENDANGERMENT_REASONS.put("M", new Qname("MKV.endangermentReasonM"));
		ENDANGERMENT_REASONS.put("Mp", new Qname("MKV.endangermentReasonMp"));
		ENDANGERMENT_REASONS.put("Mv", new Qname("MKV.endangermentReasonMv"));
		ENDANGERMENT_REASONS.put("Mk", new Qname("MKV.endangermentReasonMk"));
		ENDANGERMENT_REASONS.put("Ml", new Qname("MKV.endangermentReasonMl"));
		ENDANGERMENT_REASONS.put("O", new Qname("MKV.endangermentReasonO"));
		ENDANGERMENT_REASONS.put("Vr", new Qname("MKV.endangermentReasonVr"));
		ENDANGERMENT_REASONS.put("Kh", new Qname("MKV.endangermentReasonKh"));
		ENDANGERMENT_REASONS.put("I", new Qname("MKV.endangermentReasonI"));
		ENDANGERMENT_REASONS.put("S", new Qname("MKV.endangermentReasonS"));
		ENDANGERMENT_REASONS.put("Kil", new Qname("MKV.endangermentReasonKil"));
		ENDANGERMENT_REASONS.put("Ris", new Qname("MKV.endangermentReasonRis"));
		ENDANGERMENT_REASONS.put("Kv", new Qname("MKV.endangermentReasonKv"));
		ENDANGERMENT_REASONS.put("U", new Qname("MKV.endangermentReasonU"));
		ENDANGERMENT_REASONS.put("Vie", new Qname("MKV.endangermentReasonVie"));
		ENDANGERMENT_REASONS.put("Muu", new Qname("MKV.endangermentReasonMuu"));
		ENDANGERMENT_REASONS.put("?", new Qname("MKV.endangermentReasonT"));
	}

	private static final Map<String, Qname> STATUS_CHANGE_REASONS;
	static {
		STATUS_CHANGE_REASONS = new HashMap<>();
		STATUS_CHANGE_REASONS.put("1", new Qname("MKV.reasonForStatusChangeGenuine"));
		STATUS_CHANGE_REASONS.put("2", new Qname("MKV.reasonForStatusChangeMoreInformation"));
		STATUS_CHANGE_REASONS.put("3", new Qname("MKV.reasonForStatusChangeChangesInCriteria"));
		STATUS_CHANGE_REASONS.put("4", new Qname("MKV.reasonForStatusChangeMoreInformation"));
		STATUS_CHANGE_REASONS.put("5", new Qname("MKV.reasonForStatusChangeChangesInTaxonomy"));
		STATUS_CHANGE_REASONS.put("6", new Qname("MKV.reasonForStatusChangeError"));
		STATUS_CHANGE_REASONS.put("7", new Qname("MKV.reasonForStatusChangeErroneousInformation"));
		STATUS_CHANGE_REASONS.put("8", new Qname("MKV.reasonForStatusChangeOther"));
		STATUS_CHANGE_REASONS.put("aito muutos", new Qname("MKV.reasonForStatusChangeGenuine"));
		STATUS_CHANGE_REASONS.put("tiedon lisääntyminen", new Qname("MKV.reasonForStatusChangeMoreInformation"));
		STATUS_CHANGE_REASONS.put("tiedon kasvu", new Qname("MKV.reasonForStatusChangeMoreInformation"));
		STATUS_CHANGE_REASONS.put("taksonominen muutos", new Qname("MKV.reasonForStatusChangeChangesInTaxonomy"));
	}

	private static final Map<String, Qname> RED_LIST_STATUSES;
	static {
		RED_LIST_STATUSES = new HashMap<>();
		for (String s : "EX,EW,RE,CR,EN,VU,NT,LC,DD,NA,NE".split(",")) {
			RED_LIST_STATUSES.put(s, new Qname("MX.iucn"+s));
		}
	}

	public String getTaxonQname() {
		if (taxonQname == null) return "";
		return taxonQname;
	}

	public Qname getExteralPopulationImpactOnRedListStatus() {
		if (!given(exteralPopulationImpactOnRedListStatus)) {
			return null;
		}
		if (exteralPopulationImpactOnRedListStatus.equals("-1")) return new Qname("MKV.exteralPopulationImpactOnRedListStatusEnumMinus1");
		if (exteralPopulationImpactOnRedListStatus.equals("-2")) return new Qname("MKV.exteralPopulationImpactOnRedListStatusEnumMinus2");
		if (exteralPopulationImpactOnRedListStatus.equals("1")) return new Qname("MKV.exteralPopulationImpactOnRedListStatusEnumPlus1");
		if (exteralPopulationImpactOnRedListStatus.equals("2")) return new Qname("MKV.exteralPopulationImpactOnRedListStatusEnumPlus2");
		return null;
	}

}
