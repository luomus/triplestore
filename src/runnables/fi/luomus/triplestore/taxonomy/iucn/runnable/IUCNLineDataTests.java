package fi.luomus.triplestore.taxonomy.iucn.runnable;

import static org.junit.Assert.assertEquals;

import java.util.regex.Pattern;

import org.junit.Test;

import fi.luomus.commons.containers.rdf.Qname;

public class IUCNLineDataTests {

	@Test
	public void cleanSciname() {
		assertEquals(null, IUCN2010Sisaan.cleanScientificName("Cricotopus slossonae"));
		assertEquals("Cricotopus slossonae", IUCN2010Sisaan.cleanScientificName("Cricotopus (Cricotopus) slossonae"));
	}

	@Test
	public void test_minmax() {
		IUCNLineData data = new IUCNLineData(new String[] {});
		assertEquals(null, data.getDistributionAreaMin());
		assertEquals(null, data.getDistributionAreaMax());

		data.distributionArea = ">20000";
		assertEquals(20000, data.getDistributionAreaMin().intValue());
		assertEquals(null, data.getDistributionAreaMax());

		data.distributionArea = "yli 20000";
		assertEquals(20000, data.getDistributionAreaMin().intValue());
		assertEquals(null, data.getDistributionAreaMax());

		data.distributionArea = "4";
		assertEquals(4, data.getDistributionAreaMin().intValue());

		data.distributionArea = ">40.000";
		assertEquals(40000, data.getDistributionAreaMin().intValue());
		assertEquals(null, data.getDistributionAreaMax());

		data.distributionArea = "<40.000";
		assertEquals(null, data.getDistributionAreaMin());
		assertEquals(40000, data.getDistributionAreaMax().intValue());

		data.distributionArea = "iso";
		assertEquals(null, data.getDistributionAreaMin());
		assertEquals(null, data.getDistributionAreaMax());

		data.distributionArea = "200 000 - 300 000 km2";
		assertEquals(200000, data.getDistributionAreaMin().intValue());
		assertEquals(300000, data.getDistributionAreaMax().intValue());

		data.distributionArea = "Noin 100000";
		assertEquals(100000, data.getDistributionAreaMin().intValue());
		assertEquals(null, data.getDistributionAreaMax());

		data.distributionArea = "n. 15 000 km2 4d";
		assertEquals(null, data.getDistributionAreaMin());
		assertEquals(null, data.getDistributionAreaMax());

		data.distributionArea = "0-4";
		assertEquals(0, data.getDistributionAreaMin().intValue());
		assertEquals(4, data.getDistributionAreaMax().intValue());
	}

	@Test
	public void test_habitats() {
		IUCNLineData data = new IUCNLineData(new String[] {});
		data.primaryHabitat = "Kk, Ml, Ip"; // Kk can not be remapped
		data.secondaryHabitats = "Mktvp";

		assertEquals(null, data.getPrimaryHabitat());
		assertEquals("[null : MKV.habitatMl : null, null : MKV.habitatIp : null, null : MKV.habitatMkt : [MKV.habitatSpecificTypeV, MKV.habitatSpecificTypeP]]", data.getSecondaryHabitats().toString());

		assertEquals("", data.getHabitatGeneralNotes());
		assertEquals("Ensisijainen: Kk, Ml, Ip; Muut: Mktvp", data.getHabitatNotes());
	}

	@Test
	public void test_habitats_2() {
		IUCNLineData data = new IUCNLineData(new String[] {});
		data.primaryHabitat = "Ml, Ip";
		data.secondaryHabitats = "";

		assertEquals("null : MKV.habitatMl : null", data.getPrimaryHabitat().toString());
		assertEquals("[null : MKV.habitatIp : null]", data.getSecondaryHabitats().toString());
	}

	@Test
	public void test_habitats_3() {
		IUCNLineData data = new IUCNLineData(new String[] {});
		data.primaryHabitat = "Kpa"; // K is not mapped
		data.secondaryHabitats = "";

		assertEquals(null, data.getPrimaryHabitat());
		assertEquals("[]", data.getSecondaryHabitats().toString());
	}

	@Test
	public void test_habitats_4() {
		IUCNLineData data = new IUCNLineData(new String[] {});
		data.primaryHabitat = "Klpa";
		data.secondaryHabitats = "";

		assertEquals("null : MKV.habitatKl : [MKV.habitatSpecificTypePAK]", data.getPrimaryHabitat().toString());
		assertEquals("[]", data.getSecondaryHabitats().toString());
	}

	@Test
	public void test_generatioNAge() {
		IUCNLineData data = new IUCNLineData(new String[] {});
		data.generationAge = "1";
		assertEquals(1, data.getGenerationAge().doubleValue(), 0);
		assertEquals("", data.getGenerationAgeNotes());

		data.generationAge = "25";
		assertEquals(25, data.getGenerationAge().doubleValue(), 0);

		data.generationAge = "1-2";
		assertEquals(1.5, data.getGenerationAge().doubleValue(), 0);
		assertEquals("1-2", data.getGenerationAgeNotes());

		data.generationAge = "<3,3";
		assertEquals(3.3, data.getGenerationAge().doubleValue(), 0);
		assertEquals("<3,3", data.getGenerationAgeNotes());

		data.generationAge = "?";
		assertEquals(null, data.getGenerationAge());
		assertEquals("?", data.getGenerationAgeNotes());

		data.generationAge = "3,3";
		assertEquals(3.3, data.getGenerationAge().doubleValue(), 0);
		assertEquals("", data.getGenerationAgeNotes());

		data.generationAge = "0,5";
		assertEquals(0.5, data.getGenerationAge().doubleValue(), 0);
		assertEquals("", data.getGenerationAgeNotes());

		data.generationAge = "1-vuotinen";
		assertEquals(1, data.getGenerationAge().doubleValue(), 0);
		assertEquals("1-vuotinen", data.getGenerationAgeNotes());

		data.generationAge = "2 v";
		assertEquals(2, data.getGenerationAge().doubleValue(), 0);

		data.generationAge = "0,5-1";
		assertEquals(0.75, data.getGenerationAge().doubleValue(), 0);

		data.generationAge = "2";
		assertEquals(2, data.getGenerationAge().doubleValue(), 0);

		data.generationAge = "1-";
		assertEquals(1, data.getGenerationAge().doubleValue(), 0);

		data.generationAge = "1 (0.5)";
		assertEquals(1, data.getGenerationAge().doubleValue(), 0);

		data.generationAge = "Monivuotinen (RK 1998)";
		assertEquals(null, data.getGenerationAge());

		data.generationAge = "2-vuotinen";
		assertEquals(2, data.getGenerationAge().doubleValue(), 0);

		data.generationAge = "2 sp/vuosi";
		assertEquals(2, data.getGenerationAge().doubleValue(), 0);

		data.generationAge = "1 tai 2";
		assertEquals(null, data.getGenerationAge());

		data.generationAge = "1-2-vuotinen";
		assertEquals(1.5, data.getGenerationAge().doubleValue(), 0);
	}

	@Test
	public void test_evaluationPeriodLength() {
		IUCNLineData data = new IUCNLineData(new String[] {});
		data.evaluationPeriodLength = "0";
		assertEquals(0, data.getEvaluationPeriodLength().intValue());

		data.evaluationPeriodLength = "10(-30)";
		assertEquals(10, data.getEvaluationPeriodLength().intValue());

		data.evaluationPeriodLength = "75";
		assertEquals(75, data.getEvaluationPeriodLength().intValue());

		data.evaluationPeriodLength = "10 (-30)";
		assertEquals(10, data.getEvaluationPeriodLength().intValue());

		data.evaluationPeriodLength = "10-45";
		assertEquals(10, data.getEvaluationPeriodLength().intValue());

		data.evaluationPeriodLength = "ruots 3,3-15 v";
		assertEquals(3, data.getEvaluationPeriodLength().intValue());

		data.evaluationPeriodLength = "75 (MK)";
		assertEquals(75, data.getEvaluationPeriodLength().intValue());

		data.evaluationPeriodLength = "10-45 v";
		assertEquals(10, data.getEvaluationPeriodLength().intValue());

		data.evaluationPeriodLength = "10? (MK)";
		assertEquals(10, data.getEvaluationPeriodLength().intValue());

		data.evaluationPeriodLength = "30 (MK) (2000: 10)";
		assertEquals(30, data.getEvaluationPeriodLength().intValue());

		data.evaluationPeriodLength = "20? (MK)";
		assertEquals(20, data.getEvaluationPeriodLength().intValue());

		data.evaluationPeriodLength = "30  (EK, KM & MK)";
		assertEquals(30, data.getEvaluationPeriodLength().intValue());

		data.evaluationPeriodLength = "30 (MK; Ruotsissa käytetty 30)";
		assertEquals(30, data.getEvaluationPeriodLength().intValue());

		data.evaluationPeriodLength = "50-100?";
		assertEquals(50, data.getEvaluationPeriodLength().intValue());

		data.evaluationPeriodLength = "ruots 10 v.";
		assertEquals(10, data.getEvaluationPeriodLength().intValue());

		data.evaluationPeriodLength = "60-75?";
		assertEquals(60, data.getEvaluationPeriodLength().intValue());

		data.evaluationPeriodLength = "10 (-> ?)";
		assertEquals(10, data.getEvaluationPeriodLength().intValue());

		data.evaluationPeriodLength = "n. 20-25";
		assertEquals(20, data.getEvaluationPeriodLength().intValue());

		data.evaluationPeriodLength = "20-30?";
		assertEquals(20, data.getEvaluationPeriodLength().intValue());

		data.evaluationPeriodLength = "10/30 v";
		assertEquals(10, data.getEvaluationPeriodLength().intValue());

		data.evaluationPeriodLength = "60 vuotta";
		assertEquals(60, data.getEvaluationPeriodLength().intValue());	
	}

	@Test
	public void alternativeFinnishNames() {
		IUCNLineData data = new IUCNLineData(new String[] {});
		data.alternativeFinnishNames = "foo,bar, juusto";
		assertEquals("[foo, bar, juusto]", data.getAlternativeFinnishNames().toString());
	}

	@Test
	public void typeOfOccurrence() {
		IUCNLineData data = new IUCNLineData(new String[] {});
		assertEquals(null, data.getTypeOfOccurrenceInFinland());
		data.typeOfOccurrenceInFinland = "";
		assertEquals(null, data.getTypeOfOccurrenceInFinland());

		data.typeOfOccurrenceInFinland = "lisääntyvä";
		assertEquals("MX.typeOfOccurrenceStablePopulation", data.getTypeOfOccurrenceInFinland().toString());

		data.typeOfOccurrenceInFinland = "Alkuperäinen";
		assertEquals("MX.typeOfOccurrenceStablePopulation", data.getTypeOfOccurrenceInFinland().toString());

		data.typeOfOccurrenceInFinland = "Vakinainen";
		assertEquals("MX.typeOfOccurrenceStablePopulation", data.getTypeOfOccurrenceInFinland().toString());

		data.typeOfOccurrenceInFinland = "alkuperäinen";
		assertEquals("MX.typeOfOccurrenceStablePopulation", data.getTypeOfOccurrenceInFinland().toString());

		data.typeOfOccurrenceInFinland = "vakinainen";
		assertEquals("MX.typeOfOccurrenceStablePopulation", data.getTypeOfOccurrenceInFinland().toString());

		data.typeOfOccurrenceInFinland = "Uustulokas, vakiintunut";
		assertEquals("MX.typeOfOccurrenceStablePopulation", data.getTypeOfOccurrenceInFinland().toString());

		data.typeOfOccurrenceInFinland = "Muinaistulokas";
		assertEquals("MX.typeOfOccurrenceStablePopulation", data.getTypeOfOccurrenceInFinland().toString());

		data.typeOfOccurrenceInFinland = "<1871";
		assertEquals("MX.typeOfOccurrenceStablePopulation", data.getTypeOfOccurrenceInFinland().toString());

		data.typeOfOccurrenceInFinland = "Alkuperäinen, lisääntyvä";
		assertEquals("MX.typeOfOccurrenceStablePopulation", data.getTypeOfOccurrenceInFinland().toString());

		data.typeOfOccurrenceInFinland = "hävinnyt";
		assertEquals("MX.typeOfOccurrenceExtirpated", data.getTypeOfOccurrenceInFinland().toString());

		data.typeOfOccurrenceInFinland = "?lisääntyvä";
		assertEquals("MX.typeOfOccurrenceStablePopulation", data.getTypeOfOccurrenceInFinland().toString());

		data.typeOfOccurrenceInFinland = "Koristekasvi";
		assertEquals("MX.typeOfOccurrenceAnthropogenic", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "Koristepensas";
		assertEquals("MX.typeOfOccurrenceAnthropogenic", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "Sisätiloissa";
		assertEquals("MX.typeOfOccurrenceAnthropogenic", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "Vieraslaji";
		assertEquals("MX.typeOfOccurrenceAnthropogenic", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "Vain sisätiloissa";
		assertEquals("MX.typeOfOccurrenceAnthropogenic", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "Importti?";
		assertEquals("MX.typeOfOccurrenceAnthropogenic", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "Importti";
		assertEquals("MX.typeOfOccurrenceAnthropogenic", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "Koristepuu";
		assertEquals("MX.typeOfOccurrenceAnthropogenic", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "indoor species";
		assertEquals("MX.typeOfOccurrenceAnthropogenic", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "lisääntyvä vieraslaji";
		assertEquals("MX.typeOfOccurrenceAnthropogenic", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "Viljakasvi";
		assertEquals("MX.typeOfOccurrenceAnthropogenic", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "Vihanneskasvi";
		assertEquals("MX.typeOfOccurrenceAnthropogenic", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "Vieraslaji ";
		assertEquals("MX.typeOfOccurrenceAnthropogenic", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "Viljelymetsä- ja koristepuu";
		assertEquals("MX.typeOfOccurrenceAnthropogenic", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "Marjapensas";
		assertEquals("MX.typeOfOccurrenceAnthropogenic", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "Viljelykasvi";
		assertEquals("MX.typeOfOccurrenceAnthropogenic", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "Mauste- ja vihanneskasvi";
		assertEquals("MX.typeOfOccurrenceAnthropogenic", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "hävinnyt";
		assertEquals("MX.typeOfOccurrenceExtirpated", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "Hävinnyt?";
		assertEquals(null, data.getTypeOfOccurrenceInFinland());
		data.typeOfOccurrenceInFinland = "Hävinnyt";
		assertEquals("MX.typeOfOccurrenceExtirpated", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "Muinaistulokas, hävinnyt";
		assertEquals("MX.typeOfOccurrenceExtirpated", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "Muinaistulokas, hävinnyt?";
		assertEquals(null, data.getTypeOfOccurrenceInFinland());
		data.typeOfOccurrenceInFinland = "Uusi laji";
		assertEquals("MX.typeOfOccurrenceNotEstablished", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "Uustulokas";
		assertEquals("MX.typeOfOccurrenceNotEstablished", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "Uutena 2008";
		assertEquals("MX.typeOfOccurrenceNotEstablished", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "Uustulokas, satunnainen";
		assertEquals("MX.typeOfOccurrenceRareVagrant", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "satunnainen";
		assertEquals("MX.typeOfOccurrenceRareVagrant", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "tilapäinen";
		assertEquals("MX.typeOfOccurrenceRareVagrant", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "?satunnainen";
		assertEquals("MX.typeOfOccurrenceRareVagrant", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "Satunnainen vierailija";
		assertEquals("MX.typeOfOccurrenceRareVagrant", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "levähtävä";
		assertEquals("MX.typeOfOccurrenceRareVagrant", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "lisääntyvä";
		assertEquals("MX.typeOfOccurrenceStablePopulation", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "Alkuperäinen";
		assertEquals("MX.typeOfOccurrenceStablePopulation", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "Vakinainen";
		assertEquals("MX.typeOfOccurrenceStablePopulation", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "alkuperäinen";
		assertEquals("MX.typeOfOccurrenceStablePopulation", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "vakinainen";
		assertEquals("MX.typeOfOccurrenceStablePopulation", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "Uustulokas, vakiintunut";
		assertEquals("MX.typeOfOccurrenceStablePopulation", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "Muinaistulokas";
		assertEquals("MX.typeOfOccurrenceStablePopulation", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "Alkuperäinen, lisääntyvä";
		assertEquals("MX.typeOfOccurrenceStablePopulation", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "?lisääntyvä";
		assertEquals("MX.typeOfOccurrenceStablePopulation", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "Vakituinen, lisääntyvä";
		assertEquals("MX.typeOfOccurrenceStablePopulation", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "vakinainen/ < 1861";
		assertEquals("MX.typeOfOccurrenceStablePopulation", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "alkuperäinen?";
		assertEquals("MX.typeOfOccurrenceStablePopulation", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "vakinainen/ < 1920";
		assertEquals("MX.typeOfOccurrenceStablePopulation", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "vakinainen/ 2002";
		assertEquals("MX.typeOfOccurrenceStablePopulation", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "Vakinainen (2004)";
		assertEquals("MX.typeOfOccurrenceStablePopulation", data.getTypeOfOccurrenceInFinland().toString());
		data.typeOfOccurrenceInFinland = "Tieto puuttuu";
		assertEquals(null, data.getTypeOfOccurrenceInFinland());
		data.typeOfOccurrenceInFinland = "1 havainto";
		assertEquals(null, data.getTypeOfOccurrenceInFinland());
		data.typeOfOccurrenceInFinland = "tulokas";
		assertEquals(null, data.getTypeOfOccurrenceInFinland());
		data.typeOfOccurrenceInFinland = "ei tiedossa";
		assertEquals(null, data.getTypeOfOccurrenceInFinland());
		data.typeOfOccurrenceInFinland = "?";
		assertEquals(null, data.getTypeOfOccurrenceInFinland());
		data.typeOfOccurrenceInFinland = "1 havainto?";
		assertEquals(null, data.getTypeOfOccurrenceInFinland());
		data.typeOfOccurrenceInFinland = "todennäköisesti vakinainen";
		assertEquals(null, data.getTypeOfOccurrenceInFinland());
		data.typeOfOccurrenceInFinland = "Viirihaavista;tilapäisviipyjä?";
		assertEquals(null, data.getTypeOfOccurrenceInFinland());
		data.typeOfOccurrenceInFinland = "Epävarma";
		assertEquals(null, data.getTypeOfOccurrenceInFinland());
		data.typeOfOccurrenceInFinland = "Tulokas?";
		assertEquals(null, data.getTypeOfOccurrenceInFinland());
		data.typeOfOccurrenceInFinland = "Vakinainen?";
		assertEquals(null, data.getTypeOfOccurrenceInFinland());
		data.typeOfOccurrenceInFinland = "hävinnyt ?";
		assertEquals(null, data.getTypeOfOccurrenceInFinland());
		data.typeOfOccurrenceInFinland = "Tulokas";
		assertEquals(null, data.getTypeOfOccurrenceInFinland());
		data.typeOfOccurrenceInFinland = "Tulokas x alkuperäinen";
		assertEquals(null, data.getTypeOfOccurrenceInFinland());
		data.typeOfOccurrenceInFinland = "Mahdollisesti hävinnyt";
		assertEquals(null, data.getTypeOfOccurrenceInFinland());
		data.typeOfOccurrenceInFinland = "leviämässä?";
		assertEquals(null, data.getTypeOfOccurrenceInFinland());
		data.typeOfOccurrenceInFinland = "Palkokasvi";
		assertEquals(null, data.getTypeOfOccurrenceInFinland());
		data.typeOfOccurrenceInFinland = "?tilapäinen";
		assertEquals(null, data.getTypeOfOccurrenceInFinland());
		data.typeOfOccurrenceInFinland = "Osin sisätiloissa";
		assertEquals(null, data.getTypeOfOccurrenceInFinland());
		data.typeOfOccurrenceInFinland = "Ei tietoa";
		assertEquals(null, data.getTypeOfOccurrenceInFinland());
		data.typeOfOccurrenceInFinland = "leviämässä";
		assertEquals(null, data.getTypeOfOccurrenceInFinland());

	}

	@Test
	public void occurrences() {
		IUCNLineData data = new IUCNLineData(new String[] {});
		assertEquals("{}", data.getOccurrences().toString());
		data.occurences.put(new Qname("ML.690"), "");
		assertEquals("{}", data.getOccurrences().toString());

		data.occurences.put(new Qname("ML.690"), "X");
		assertEquals("{ML.690=MX.typeOfOccurrenceOccurs}", data.getOccurrences().toString());

		data.occurences.put(new Qname("ML.690"), "x");
		assertEquals("{ML.690=MX.typeOfOccurrenceOccurs}", data.getOccurrences().toString());

		data.occurences.put(new Qname("ML.690"), "-");
		assertEquals("{ML.690=MX.doesNotOccur}", data.getOccurrences().toString());

		data.occurences.put(new Qname("ML.690"), "  -");
		assertEquals("{}", data.getOccurrences().toString());

		data.occurences.put(new Qname("ML.690"), "+");
		assertEquals("{ML.690=MX.typeOfOccurrenceOccurs}", data.getOccurrences().toString());

		data.occurences.put(new Qname("ML.690"), "?");
		assertEquals("{ML.690=MX.typeOfOccurrenceUncertain}", data.getOccurrences().toString());

		data.occurences.put(new Qname("ML.690"), "_");
		assertEquals("{ML.690=MX.doesNotOccur}", data.getOccurrences().toString());

		data.occurences.put(new Qname("ML.690"), "­");
		assertEquals("{ML.690=MX.doesNotOccur}", data.getOccurrences().toString());

		data.occurences.put(new Qname("ML.690"), "RE");
		assertEquals("{ML.690=MX.typeOfOccurrenceExtirpated}", data.getOccurrences().toString());

		data.occurences.put(new Qname("ML.690"), "RT");
		assertEquals("{ML.690=MX.typeOfOccurrenceOccurs}", data.getOccurrences().toString());

		data.occurences.put(new Qname("ML.690"), "NE");
		assertEquals("{}", data.getOccurrences().toString());

		data.occurences.put(new Qname("ML.690"), "0");
		assertEquals("{}", data.getOccurrences().toString());

		data.occurences.put(new Qname("ML.690"), "*-");
		assertEquals("{}", data.getOccurrences().toString());

		data.occurences.put(new Qname("ML.690"), "X?");
		assertEquals("{ML.690=MX.typeOfOccurrenceUncertain}", data.getOccurrences().toString());

		data.occurences.put(new Qname("ML.690"), "RE?");
		assertEquals("{ML.690=MX.typeOfOccurrenceUncertain}", data.getOccurrences().toString());

		data.occurences.put(new Qname("ML.690"), "NE?");
		assertEquals("{ML.690=MX.typeOfOccurrenceUncertain}", data.getOccurrences().toString());

		data.occurences.put(new Qname("ML.690"), "RT?");
		assertEquals("{ML.690=MX.typeOfOccurrenceUncertain}", data.getOccurrences().toString());

		data.occurences.put(new Qname("ML.690"), "(x)");
		assertEquals("{ML.690=MX.typeOfOccurrenceUncertain}", data.getOccurrences().toString());

		assertEquals("1a: (x), 1b: , 2a: , 2b: , 3a: , 3b: , 3c: , 4a: , 4b: , 4c: , 4d: ", data.getOccurrenceRegionsNotes());
	}

	@Test
	public void booleanV() {
		IUCNLineData data = new IUCNLineData(new String[] {});
		assertEquals(null, data.getPopulationVaries());

		data.populationVaries = "x";
		assertEquals(true, data.getPopulationVaries());

		data.populationVaries = "Kyllä";
		assertEquals(true, data.getPopulationVaries());

		data.populationVaries = "Ei";
		assertEquals(false, data.getPopulationVaries());

		data.populationVaries = "blaa";
		assertEquals(null, data.getPopulationVaries());
	}

	@Test
	public void endangermentreason() {
		IUCNLineData data = new IUCNLineData(new String[] {});
		assertEquals("[]", data.getEndangermentReasons().toString());

		data.endangermentReasons = "Ml, Mv, xx";
		assertEquals("[MKV.endangermentReasonMl, MKV.endangermentReasonMv]", data.getEndangermentReasons().toString());
	}

	@Test
	public void status() {
		IUCNLineData data = new IUCNLineData(new String[] {});
		assertEquals(null, data.getRedListStatus());

		data.redListStatus = "LC";
		assertEquals("MX.iucnLC", data.getRedListStatus().toString());

		data.redListStatus = "VU*";
		assertEquals("MX.iucnVU", data.getRedListStatus().toString());

		data.redListStatusRange = "LC-NT";
		assertEquals("MX.iucnLC", data.getRedListStatusMin().toString());
		assertEquals("MX.iucnNT", data.getRedListStatusMax().toString());

		data.redListStatusRange = "LC";
		assertEquals("MX.iucnLC", data.getRedListStatusMin().toString());
		assertEquals("MX.iucnLC", data.getRedListStatusMax().toString());
	}

	@Test
	public void bugfix() {
		String line = "OR12|Tetrix fuliginosa |(Zetterstedt, 1828)||Tetrigidae|Orthoptera|Insecta|Arthropoda|||suorasiipiset||lapinokasirkka||norlig torngräshoppa|||lisääntyvä|130 000|4-?|||||x|x|x|x|x|x|x|It|Rjn|Kosteat avoimet paikat, niityt, pientareet. Ei soilla.|esiintyminen huonosti tunnettu, ainoa uusi havainto 2008 Rovaniemi, edellinen vuodelta 1956||10|||||tiedot puutteellisia, mahd. taantuva||kyllä|ei|||-|? B2ab(ii,iii,iv)|-|-|-|Laaja levinneisyys,   harvinainen ja mahdollisesti taantunut (vain 1 uusi havainto). Vaikea löytää ja tunnistaa. Todennäköisen taantumisen ja pirstoutumisen perusteella täyttäisi kriteerin B. Puutteellisesti etsitty, paikkoja voi olla aika paljonkin.|LC||DD| |4|LC-EN|epävarma, ?kannanmuutos|||||||||";
		IUCNLineData data = new IUCNLineData(line.split(Pattern.quote("|")));
		assertEquals("4-?", data.getOccurrenceAreaNotes());
		assertEquals(4, data.getOccurrenceAreaMin().intValue());
		assertEquals(null, data.getOccurrenceAreaMax());
	}

}