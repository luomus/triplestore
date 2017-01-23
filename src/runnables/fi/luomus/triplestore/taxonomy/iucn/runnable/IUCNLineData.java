package fi.luomus.triplestore.taxonomy.iucn.runnable;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNHabitatObject;

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
			return parts[i].trim();
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
		String s = typeOfOccurrenceInFinland.toLowerCase().replace("?", "").replace("<", "").replace(">", "").trim();
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

	// TODO test primary, secondary habitats
	
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
				list.add(i.next());
			}
		}
		if (given(secondaryHabitats)) {
			list.addAll(getHabitats(secondaryHabitats));
		}
		return list;
	}

	private List<IUCNHabitatObject> getHabitats(String habitats) {
		List<IUCNHabitatObject> list = new ArrayList<>();
		habitats = Utils.removeWhitespace(habitats).replace("(", "").replace(")", "").replace("?", "").replace("*", "");
		Set<String> unique = new LinkedHashSet<>();
		for (String s : habitats.split(",")) {
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
				IUCNHabitatObject habitatObject = new IUCNHabitatObject(null, habitatId, i++);
				for (Qname specificType : specificTypes) {
					habitatObject.addHabitatSpecificType(specificType);
				}
				list.add(habitatObject);
			}
		}
		return list;
	}

	private Object[] getSpecificTypesFromEnd(String s) {
		Set<Qname> types = new HashSet<>();
		while (true) {
			if (HABITAS.containsKey(s)) break; // shortened string is now a valid habitat
			for (String type : HABITAT_SPECIFIC_TYPES.keySet()) {
				if (s.endsWith(type)) {
					types.add(HABITAT_SPECIFIC_TYPES.get(type));
					s = s.substring(0, s.length() - type.length());
					break;
				}
			}
			break; // nothing new found
		}
		return new Object[] { s, types};
	}

	public String getHabitatNotes() {
		StringBuilder b = new StringBuilder();
		if (given(primaryHabitat)) b.append("Ensisijaiset: " + primaryHabitat);
		if (given(secondaryHabitats)) b.append(" Toissijaiset: ").append(secondaryHabitats);
		return b.toString();
	}
	public String getHabitatGeneralNotes() {
		return habitatNotes;
	}

	public String getOccurrenceNotes {
		return occurrenceNotes;
	}
	
	public Double getGenerationAge() {
		
	}
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

	private Integer getMin(String s) {
		s = cleanMinmax(s);
		if (s.startsWith(">")) return iVal(s.replace(">", ""));
		if (s.startsWith("<")) return null;
		return iVal(s);
	}

	private Integer getMax(String s) {
		s = cleanMinmax(s);
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
		return s.replace("±2", "").replace("n.", "").replace("km²", "").replace("km2", "").replace(".", "").replace(" ", "").replace("Noin", "").replace("arv.", "").replace("Selvästi", "").replace("yli", ">").replace("?", "").replace("tunnettu", "").replace("alle","<").replace("Ehkä", "").trim();
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




}
