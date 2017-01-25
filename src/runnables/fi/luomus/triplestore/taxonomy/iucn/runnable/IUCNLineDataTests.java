package fi.luomus.triplestore.taxonomy.iucn.runnable;

import static org.junit.Assert.assertEquals;

import fi.luomus.commons.containers.rdf.Qname;

import org.junit.Test;

public class IUCNLineDataTests {

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
		assertEquals(0, data.getEvaluationPeriod().intValue());
		
		data.evaluationPeriodLength = "10(-30)";
		assertEquals(10, data.getEvaluationPeriod().intValue());
		
		data.evaluationPeriodLength = "75";
		assertEquals(75, data.getEvaluationPeriod().intValue());
		
		data.evaluationPeriodLength = "10 (-30)";
		assertEquals(10, data.getEvaluationPeriod().intValue());
		
		data.evaluationPeriodLength = "10-45";
		assertEquals(10, data.getEvaluationPeriod().intValue());
		
		data.evaluationPeriodLength = "ruots 3,3-15 v";
		assertEquals(3, data.getEvaluationPeriod().intValue());
		
		data.evaluationPeriodLength = "75 (MK)";
		assertEquals(75, data.getEvaluationPeriod().intValue());
		
		data.evaluationPeriodLength = "10-45 v";
		assertEquals(10, data.getEvaluationPeriod().intValue());
		
		data.evaluationPeriodLength = "10? (MK)";
		assertEquals(10, data.getEvaluationPeriod().intValue());
		
		data.evaluationPeriodLength = "30 (MK) (2000: 10)";
		assertEquals(30, data.getEvaluationPeriod().intValue());
		
		data.evaluationPeriodLength = "20? (MK)";
		assertEquals(20, data.getEvaluationPeriod().intValue());
		
		data.evaluationPeriodLength = "30  (EK, KM & MK)";
		assertEquals(30, data.getEvaluationPeriod().intValue());
		
		data.evaluationPeriodLength = "30 (MK; Ruotsissa käytetty 30)";
		assertEquals(30, data.getEvaluationPeriod().intValue());
		
		data.evaluationPeriodLength = "50-100?";
		assertEquals(50, data.getEvaluationPeriod().intValue());
		
		data.evaluationPeriodLength = "ruots 10 v.";
		assertEquals(10, data.getEvaluationPeriod().intValue());
		
		data.evaluationPeriodLength = "60-75?";
		assertEquals(60, data.getEvaluationPeriod().intValue());
		
		data.evaluationPeriodLength = "10 (-> ?)";
		assertEquals(10, data.getEvaluationPeriod().intValue());
		
		data.evaluationPeriodLength = "n. 20-25";
		assertEquals(20, data.getEvaluationPeriod().intValue());
		
		data.evaluationPeriodLength = "20-30?";
		assertEquals(20, data.getEvaluationPeriod().intValue());
		
		data.evaluationPeriodLength = "10/30 v";
		assertEquals(10, data.getEvaluationPeriod().intValue());
		
		data.evaluationPeriodLength = "60 vuotta";
		assertEquals(60, data.getEvaluationPeriod().intValue());	
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
		
		data.typeOfOccurrenceInFinland = "Uustulokas, satunnainen";
		assertEquals("MX.typeOfOccurrenceNotEstablished", data.getTypeOfOccurrenceInFinland().toString());
		
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

}