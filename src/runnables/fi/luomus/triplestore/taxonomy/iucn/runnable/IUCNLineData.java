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
	public String scientificName;
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
	public String populationSizePeriodBeginning;
	public String populationSizePeriodEnd;
	public String decreaseDuringPeriod;
	public String populationVaries;
	public String fragmentedHabitats;
	public String borderGain;
	public String endangermentReasons;
	public String threats;
	public String criteriaA;
	public String criteriaB;
	public String criteriaC;
	public String criteriaD;
	public String criteriaE;
	public String groundsForEvaluationNotes;
	public String redListStatus;
	public String criteriaForStatus;
	public String reasonForStatusChange;
	public String redListStatusRange;
	public String possiblyRE;
	public String lastSightingNotes;
	public String lsaRecommendation;
	public String legacyPublications;
	private final String[] parts;

	public IUCNLineData(String[] parts) {
		this.parts = parts;
		scientificName = s(1);
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
		return s != null && s.length() > 0;
	}

	public String getTaxonomicNotes() {
		return taxonomicNotes;
	}

	public Qname getTypeOfOccurrenceInFinland() {
		if (!given(typeOfOccurrenceInFinland)) return null;
		String s = typeOfOccurrenceInFinland.toLowerCase().replace("?", "").replace("<", "").replace(">", "").replace("n.", "").trim();
		if (validInteger(s)) return STABLE; 
		if (s.contains("hävinnyt")) return EXTIRPATED;
		if (s.contains("vieraslaji")) return ANTHROPOGENIC;
		if (s.contains("vakituinen") || s.equals("muinaistulokas") || s.contains("lisääntyvä") || s.equals("alkuperäinen") || s.contains("vakinainen") || s.contains("vakiintunut")) return STABLE;
		if (s.contains("uusi laji") || s.equals("uustulokas, satunnainen") || s.contains("tulokas")) return NOT_ESTAMBLISHED;
		if (s.contains("tilapäisviipyjä")) return RARE;
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
		s = s.replace("?", "").toLowerCase();
		if (!given(s)) return null;
		if (s.equals("kyllä") || s.equals("x") || s.equals("on") || s.equals("+") || s.equals("(x)")) return true;
		if (s.equals("ei")) return false;
		if (s.contains("voimakas")) return true;
		if (s.equals("selvä")) return true;
		return null;
	}

	public String getPopulationVariesNotes() {
		return populationVaries;
	}

	public Boolean getFragmentedHabitats() {
		return bVal(fragmentedHabitats);
	}

	public String getFragmentedHabitatsNotes() {
		return fragmentedHabitats;
	}

	public Boolean getBorderGain() {
		return bVal(borderGain);
	}

	public String getBorderGainNotes() {
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

	public String getCriteriaA() { return criteriaA; }
	public String getCriteriaB() { return criteriaB; }
	public String getCriteriaC() { return criteriaC; }
	public String getCriteriaD() { return criteriaD; }
	public String getCriteriaE() { return criteriaE; }
	public String getGroundsForEvaluationNotes() { return groundsForEvaluationNotes; }
	public String getCriteriaForStatus() {return criteriaForStatus; }

	public List<Qname> getReasonForStatusChange() {
		if (!given(reasonForStatusChange)) return Collections.emptyList();
		String s = Utils.removeWhitespace(reasonForStatusChange);
		List<Qname> list = new ArrayList<>();
		for (String part : s.split(Pattern.quote(","))) {
			Qname r = STATUS_CHANGE_REASONS.get(part);
			if (r != null) list.add(r);
		}
		return list;
	}

	public String getReasonForStatusChangeNotes() {
		return reasonForStatusChange;
	}

	public Qname getRedListStatus() {
		String s = redListStatus.replace(".", "").replace("*", "").trim();
		return RED_LIST_STATUSES.get(s);
	}

	public String getRedListStatusNotes() {
		if (getRedListStatus() == null) return redListStatus;
		return "";
	}

	public Qname getRedListStatusMin() {
		return RED_LIST_STATUSES.get(redListStatusRange.split("-")[0]);
	}

	public Qname getRedListStatusMax() {
		if (!redListStatusRange.contains("-")) return getRedListStatusMin();
		return RED_LIST_STATUSES.get(redListStatusRange.split("-")[1]);
	}


	public Boolean getPossiblyRE() {
		String s = possiblyRE.toLowerCase();
		if (s.equals("re") || s.equals("kyllä")) return true;
		return null;
	}

	public String getPossiblyRENotes() {
		return possiblyRE;
	}

	public String getLastSightingNotes() {
		return lastSightingNotes;
	}

	public Boolean getLsaRecommendation() {
		String s = lsaRecommendation.toLowerCase();
		if (s.equals("e") || s.equals("e*")) return true;
		return null;
	};

	public String getLsaRecommendationNotes() {
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
		return s.toLowerCase().replace("±2", "").replace("n.", "").replace("km²", "").replace("km2", "").replace(".", "").replace(" ", "").replace("noin", "").replace("arv.", "").replace("selvästi", "").replace("yli", ">").replace("?", "").replace("tunnettu", "").replace("alle","<").replace("ehkä", "").trim();
	}

	private static final Map<String, Qname> HABITAS;
	static {
		HABITAS = new HashMap<>();
		for (String s : "Mk,Mkk,Mkt,Ml,Mlt,Mlk,S,Sl,Sla,Slr,Slk,Sn,Snk,Snr,Sr,Srk,Srr,Sk,Skk,Skr,V,Vi,Vs,Vsk,Vsr,Va,Vj,Vp,Vk,Vl,R,Ri,Rih,Ris,Rit,Rj,Rjh,Rjs,Rjt,Kl,T,Tk,Tl,Tn,I,In,Ih,Io,Iv,Ip,Ir".split(",")) {
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
		STATUS_CHANGE_REASONS.put("6", new Qname("MKV.reasonForStatusChangeChangesInTaxonomy"));
	}

	private static final Map<String, Qname> RED_LIST_STATUSES;
	static {
		RED_LIST_STATUSES = new HashMap<>();
		for (String s : "EX,EW,RE,CR,EN,VU,NT,LC,DD,NA,NE".split(",")) {
			RED_LIST_STATUSES.put(s, new Qname("MX.iucn"+s));
		}
	}
}
