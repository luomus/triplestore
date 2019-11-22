package fi.luomus.triplestore.taxonomy.iucn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fi.luomus.commons.config.Config;
import fi.luomus.commons.config.ConfigReader;
import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.reporting.ErrorReporingToSystemErr;
import fi.luomus.commons.reporting.ErrorReporter;
import fi.luomus.commons.taxonomy.Occurrences.Occurrence;
import fi.luomus.commons.taxonomy.iucn.EndangermentObject;
import fi.luomus.commons.taxonomy.iucn.Evaluation;
import fi.luomus.commons.taxonomy.iucn.HabitatObject;
import fi.luomus.triplestore.dao.DataSourceDefinition;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.dao.TriplestoreDAOConst;
import fi.luomus.triplestore.dao.TriplestoreDAOImple;
import fi.luomus.triplestore.taxonomy.iucn.model.ValidationResult;
import fi.luomus.triplestore.taxonomy.iucn.model.Validator;

public class IUCNValidatorTests {

	private static TriplestoreDAO dao;
	private static DataSource dataSource;
	private static Validator validator;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Config config = new ConfigReader("C:/apache-tomcat/app-conf/triplestore-v2.properties");
		TriplestoreDAOConst.SCHEMA = config.get("LuontoDbName");
		dataSource = DataSourceDefinition.initDataSource(config.connectionDescription());
		dao = new TriplestoreDAOImple(dataSource, TriplestoreDAO.TEST_USER, new ErrorReporingToSystemErr());
		ErrorReporter errorReporter = new ErrorReporingToSystemErr();
		validator = new Validator(dao, errorReporter);
	}

	@AfterClass
	public static void afterClass() {
		dataSource.close();
	}

	@Test
	public void notInitialized() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		Evaluation givenData = new Evaluation(givenModel, dao.getProperties(Evaluation.EVALUATION_CLASS));

		ValidationResult result = validator.validate(givenData, null);
		assertTrue(result.hasErrors());
		assertEquals(
				"[Ohjelmointivirhe: rdf:type puuttuu!, Ohjelmointivirhe: MKV.evaluatedTaxon puuttuu!, Ohjelmointivirhe: MKV.evaluationYear puuttuu!, Ohjelmointivirhe: MKV.state puuttuu!]", 
				result.listErrors().toString());
	}

	@Test
	public void correctlyInitialized() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		Evaluation givenData = createEvaluation(givenModel);

		ValidationResult result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());
	}

	private Evaluation createEvaluation(Model givenModel) throws Exception {
		Evaluation givenData = new Evaluation(givenModel, dao.getProperties(Evaluation.EVALUATION_CLASS));
		givenModel.setType(Evaluation.EVALUATION_CLASS);
		givenModel.addStatementIfObjectGiven(Evaluation.EVALUATED_TAXON, new Qname("MX.1"));
		givenModel.addStatementIfObjectGiven(Evaluation.EVALUATION_YEAR, "2000");
		givenModel.addStatementIfObjectGiven(Evaluation.STATE, new Qname(Evaluation.STATE_STARTED));
		return givenData;
	}

	private Evaluation createReadyEvaluation(Model givenModel) throws Exception {
		Evaluation givenData = createEvaluation(givenModel);
		givenModel.removeAll(new Predicate(Evaluation.STATE));
		givenModel.addStatementIfObjectGiven(Evaluation.STATE, new Qname(Evaluation.STATE_READY));
		givenData.setPrimaryHabitat(new HabitatObject(null, new Qname("Something required"), 1));
		return givenData;
	}

	@Test
	public void invalidDataTypes() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		Evaluation givenData = createEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(Evaluation.STATE, "literal");
		givenModel.addStatementIfObjectGiven(Evaluation.DISTRIBUTION_AREA_MAX, "alpha");
		givenModel.addStatementIfObjectGiven(Evaluation.RED_LIST_STATUS, new Qname("MX.invalidEnum"));
		givenModel.addStatementIfObjectGiven(Evaluation.GENERATION_AGE, "6,5");
		ValidationResult result = validator.validate(givenData, null);
		assertEquals(""+
				"[Epäkelpo luku kentässä Levinneisyysalueen koko, max.: alpha, "+
				"Epäkelpo luku kentässä Sukupolvi: 6,5, "+
				"Ohjelmointivirhe: Virheellinen arvo MX.invalidEnum muuttujalle MKV.redListStatus, "+
				"Ohjelmointivirhe: Literaali asetettu muuttujalle MKV.state jonka pitäisi olla joukossa MKV.stateEnum]", 
				result.listErrors().toString());
	}

	@Test
	public void requiredForReady() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		Evaluation givenData = createReadyEvaluation(givenModel);

		ValidationResult result = validator.validate(givenData, null);
		assertTrue(result.hasErrors());
		assertEquals(
				"[Pakollinen tieto: Luokka]", 
				result.listErrors().toString());
	}

	@Test
	public void test_min_max_integer() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		Evaluation givenData = createEvaluation(givenModel);

		givenModel.addStatementIfObjectGiven(Evaluation.OCCURRENCE_AREA_MIN, "4", null);
		ValidationResult result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());

		givenModel.addStatementIfObjectGiven(Evaluation.OCCURRENCE_AREA_MAX, "4", null);
		result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());

		givenModel.removeAll(new Predicate(Evaluation.OCCURRENCE_AREA_MAX));
		givenModel.addStatementIfObjectGiven(Evaluation.OCCURRENCE_AREA_MAX, "5", null);
		result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());

		givenModel.removeAll(new Predicate(Evaluation.OCCURRENCE_AREA_MAX));
		givenModel.addStatementIfObjectGiven(Evaluation.OCCURRENCE_AREA_MAX, "3", null);
		result = validator.validate(givenData, null);
		assertTrue(result.hasErrors());
		assertEquals("Arvovälin ala-arvo 4 ei saa olla suurempi kuin yläarvo 3", result.listErrors().get(0));
	}

	@Test
	public void test_min_max_iucn_range() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		Evaluation givenData = createEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(Evaluation.RED_LIST_STATUS_MIN, new Qname("MX.iucnLC"));
		givenModel.addStatementIfObjectGiven(Evaluation.RED_LIST_STATUS_MAX, new Qname("MX.iucnNE"));
		ValidationResult result = validator.validate(givenData, null);
		assertTrue(result.hasErrors());
		assertEquals("Arvoa \"NE – Arvioimatta jätetyt\" ei voi käyttää arvovälinä", result.listErrors().get(0));
	}

	@Test
	public void test_min_max_iucn_range_2() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		Evaluation givenData = createEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(Evaluation.RED_LIST_STATUS_MIN, new Qname("MX.iucnLC"));
		givenModel.addStatementIfObjectGiven(Evaluation.RED_LIST_STATUS_MAX, new Qname("MX.iucnVU"));
		ValidationResult result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());
	}

	@Test
	public void test_min_max_iucn_range_3() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		Evaluation givenData = createEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(Evaluation.RED_LIST_STATUS_MIN, new Qname("MX.iucnVU"));
		givenModel.addStatementIfObjectGiven(Evaluation.RED_LIST_STATUS_MAX, new Qname("MX.iucnLC"));
		ValidationResult result = validator.validate(givenData, null);
		assertTrue(result.hasErrors());
		assertEquals("Arvovälin ala-arvo \"VU – Vaarantuneet\" ei saa olla suurempi kuin yläarvo \"LC – Elinvoimaiset\"", result.listErrors().get(0));
	}

	@Test
	public void test_invasive() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		Evaluation givenData = createReadyEvaluation(givenModel);

		givenModel.addStatementIfObjectGiven(Evaluation.TYPE_OF_OCCURRENCE_IN_FINLAND, new Qname("MX.typeOfOccurrenceAnthropogenic"));
		givenModel.addStatementIfObjectGiven(Evaluation.RED_LIST_STATUS, new Qname("MX.iucnLC"));

		ValidationResult result = validator.validate(givenData, null);
		assertTrue(result.hasErrors());
		assertEquals("Vieraslajille ainut sallittu luokka on NA", result.listErrors().get(0));
	}

	@Test
	public void test_invasive_2() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		Evaluation givenData = createReadyEvaluation(givenModel);

		givenModel.addStatementIfObjectGiven(Evaluation.TYPE_OF_OCCURRENCE_IN_FINLAND, new Qname("MX.typeOfOccurrenceAnthropogenic"));
		givenModel.addStatementIfObjectGiven(Evaluation.RED_LIST_STATUS, new Qname("MX.iucnNA"));

		ValidationResult result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());
	}

	@Test
	public void test_non_invasive() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		Evaluation givenData = createReadyEvaluation(givenModel);

		givenModel.addStatementIfObjectGiven(Evaluation.TYPE_OF_OCCURRENCE_IN_FINLAND, new Qname("MX.typeOfOccurrenceOccurs"));
		givenModel.addStatementIfObjectGiven(Evaluation.RED_LIST_STATUS, new Qname("MX.iucnLC"));

		ValidationResult result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());
	}

	@Test
	public void test_criteria_and_status_1() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		Evaluation givenData = createReadyEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(Evaluation.RED_LIST_STATUS, new Qname("MX.iucnLC"));

		givenModel.addStatementIfObjectGiven(Evaluation.CRITERIA_B, "B2b(ii,iii,v)c(iv)");
		givenModel.addStatementIfObjectGiven(Evaluation.STATUS_B, new Qname("MX.iucnNT"));

		ValidationResult result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());
	}

	@Test
	public void test_criteria_and_status_2() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		Evaluation givenData = createReadyEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(Evaluation.RED_LIST_STATUS, new Qname("MX.iucnLC"));

		givenModel.addStatementIfObjectGiven(Evaluation.CRITERIA_B, "B2b(ii,iii,v)c(iv)");

		ValidationResult result = validator.validate(givenData, null);
		assertEquals("[Kriteeri B ja siitä seuraava luokka on annettava jos toinen tiedoista annetaan]", result.listErrors().toString());
	}

	@Test
	public void test_criteria_and_status_3() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		Evaluation givenData = createReadyEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(Evaluation.RED_LIST_STATUS, new Qname("MX.iucnLC"));

		givenModel.addStatementIfObjectGiven(Evaluation.STATUS_C, new Qname("MX.iucnNT"));

		ValidationResult result = validator.validate(givenData, null);
		assertEquals("[Kriteeri C ja siitä seuraava luokka on annettava jos toinen tiedoista annetaan]", result.listErrors().toString());
	}


	@Test
	public void test_criteria_B1() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		Evaluation givenData = createReadyEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(Evaluation.RED_LIST_STATUS, new Qname("MX.iucnLC"));

		givenModel.addStatementIfObjectGiven(Evaluation.DISTRIBUTION_AREA_MIN, "4");
		givenModel.addStatementIfObjectGiven(Evaluation.CRITERIA_FOR_STATUS, "B1a");

		ValidationResult result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());
	}

	@Test
	public void test_criteria_B1_2() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		Evaluation givenData = createReadyEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(Evaluation.RED_LIST_STATUS, new Qname("MX.iucnLC"));

		givenModel.addStatementIfObjectGiven(Evaluation.DISTRIBUTION_AREA_MIN, "4");
		givenModel.addStatementIfObjectGiven(Evaluation.CRITERIA_B, "B1a");
		givenModel.addStatementIfObjectGiven(Evaluation.STATUS_B, new Qname("MX.iucnVU"));

		ValidationResult result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());
	}

	@Test
	public void test_criteria_B1_3() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		Evaluation givenData = createReadyEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(Evaluation.RED_LIST_STATUS, new Qname("MX.iucnLC"));

		givenModel.addStatementIfObjectGiven(Evaluation.DISTRIBUTION_AREA_MIN, "4");

		ValidationResult result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());
	}

	public void test_criteria_B1_4() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		Evaluation givenData = createReadyEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(Evaluation.RED_LIST_STATUS, new Qname("MX.iucnLC"));

		givenModel.addStatementIfObjectGiven(Evaluation.CRITERIA_B, "B1a");

		ValidationResult result = validator.validate(givenData, null);
		assertEquals("Levinneisyysalueen koko on ilmoitettava käytettäessä kriteeriä B1", result.listErrors().toString());
	}

	public void test_criteria_B1_5() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		Evaluation givenData = createReadyEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(Evaluation.RED_LIST_STATUS, new Qname("MX.iucnLC"));

		givenModel.addStatementIfObjectGiven(Evaluation.CRITERIA_FOR_STATUS, "B1a");

		ValidationResult result = validator.validate(givenData, null);
		assertEquals("Levinneisyysalueen koko on ilmoitettava käytettäessä kriteeriä B1", result.listErrors().toString());
	}

	@Test
	public void test_criteria_various() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		Evaluation givenData = createReadyEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(Evaluation.RED_LIST_STATUS, new Qname("MX.iucnLC"));

		givenModel.addStatementIfObjectGiven(Evaluation.CRITERIA_FOR_STATUS, "A1a; B1a+2a");

		ValidationResult result = validator.validate(givenData, null);
		assertEquals("" +
				"[Levinneisyysalueen koko on ilmoitettava käytettäessä kriteeriä B1, "+
				"Esiintymisalueen koko on ilmoitettava käytettäessä kriteeriä B2, "+
				"Tarkastelujakson pituus on ilmoitettava käytettäessä kriteeriä A]", 
				result.listErrors().toString());
	}

	@Test
	public void test_occurrences_primHabitat_threats_endangerment_1() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		Evaluation givenData = createReadyEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(Evaluation.RED_LIST_STATUS, new Qname("MX.iucnVU"));
		givenData.setPrimaryHabitat(null);

		ValidationResult result = validator.validate(givenData, null);
		assertEquals("" +
				"[Esiintymisalueet on täytettävä luokille NT-CR, "+
				"Uhanalaisuuden syyt on täytettävä luokille NT-RE, "+
				"Uhkatekijät on täytettävä luokille NT-CR, "+
				"Luokkaan johtaneet kriteerit on täytettävä luokille NT-CR]",
				result.listErrors().toString());
	}

	@Test
	public void test_endangerment_not_required_for_criteria() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		Evaluation givenData = createReadyEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(Evaluation.RED_LIST_STATUS, new Qname("MX.iucnVU"));
		givenModel.addStatementIfObjectGiven(Evaluation.CRITERIA_FOR_STATUS, "A3b");

		ValidationResult result = validator.validate(givenData, null);
		assertEquals("" +
				"[Tarkastelujakson pituus on ilmoitettava käytettäessä kriteeriä A, " + 
				"Esiintymisalueet on täytettävä luokille NT-CR, "+
				"Uhkatekijät on täytettävä luokille NT-CR]",
				result.listErrors().toString());
	}

	@Test
	public void test_occurrences_primHabitat_threats_endangerment_2() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		Evaluation givenData = createReadyEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(Evaluation.RED_LIST_STATUS, new Qname("MX.iucnVU"));
		givenData.addOccurrence(new Occurrence(null, new Qname("some"), new Qname("occ")));
		givenData.addEndangermentReason(new EndangermentObject(null, new Qname("some"), 0));
		givenData.addThreat(new EndangermentObject(null, new Qname("some"), 0));
		givenData.getModel().addStatementIfObjectGiven(Evaluation.CRITERIA_FOR_STATUS, "D1");
		ValidationResult result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());
	}

	@Test
	public void test_regional_endangerment() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		Evaluation givenData = createReadyEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(Evaluation.RED_LIST_STATUS, new Qname("MX.iucnNT"));
		givenData.addOccurrence(new Occurrence(null, new Qname("some"), new Qname("MX.typeOfOccurrenceOccursButThreatened")));
		givenData.addEndangermentReason(new EndangermentObject(null, new Qname("some"), 0));
		givenData.addThreat(new EndangermentObject(null, new Qname("some"), 0));
		givenData.getModel().addStatementIfObjectGiven(Evaluation.CRITERIA_FOR_STATUS, "D1");
		ValidationResult result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());
	}

	@Test
	public void test_regional_endangerment_2() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		Evaluation givenData = createReadyEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(Evaluation.RED_LIST_STATUS, new Qname("MX.iucnVU"));
		givenData.addOccurrence(new Occurrence(null, new Qname("some"), new Qname("MX.typeOfOccurrenceOccursButThreatened")));
		givenData.addEndangermentReason(new EndangermentObject(null, new Qname("some"), 0));
		givenData.addThreat(new EndangermentObject(null, new Qname("some"), 0));
		givenData.getModel().addStatementIfObjectGiven(Evaluation.CRITERIA_FOR_STATUS, "D1");
		ValidationResult result = validator.validate(givenData, null);
		assertTrue(result.hasErrors());
		assertEquals("[Alueelliseesti uhanalaiseksi voi merkitä vain luokkiin LC ja NT määriteltyjä lajeja. Tätä uhanalaisemmat lajit ovat automaattisesti alueellisesti uhanalaisia.]", result.listErrors().toString());
	}

	@Test
	public void test_regional_endangerment_3() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		Evaluation givenData = createReadyEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(Evaluation.RED_LIST_STATUS, new Qname("MX.iucnVU"));
		givenData.addOccurrence(new Occurrence(null, new Qname("some"), new Qname("MX.typeOfOccurrenceOccurs")));
		givenData.addEndangermentReason(new EndangermentObject(null, new Qname("some"), 0));
		givenData.addThreat(new EndangermentObject(null, new Qname("some"), 0));
		givenData.getModel().addStatementIfObjectGiven(Evaluation.CRITERIA_FOR_STATUS, "D1");
		ValidationResult result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());
	}

	@Test
	public void test_evaluationPeriodLength() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		Evaluation givenData = createEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(Evaluation.RED_LIST_STATUS, new Qname("MX.iucnLC"));
		givenData.getModel().addStatementIfObjectGiven(Evaluation.EVALUATION_PERIOD_LENGTH, "10");
		ValidationResult result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());

		givenData.getModel().removeAll(new Predicate(Evaluation.EVALUATION_PERIOD_LENGTH));
		givenData.getModel().addStatementIfObjectGiven(Evaluation.EVALUATION_PERIOD_LENGTH, "100");
		result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());

		givenData.getModel().removeAll(new Predicate(Evaluation.EVALUATION_PERIOD_LENGTH));
		givenData.getModel().addStatementIfObjectGiven(Evaluation.EVALUATION_PERIOD_LENGTH, "50");
		result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());

		givenData.getModel().removeAll(new Predicate(Evaluation.EVALUATION_PERIOD_LENGTH));
		givenData.getModel().addStatementIfObjectGiven(Evaluation.EVALUATION_PERIOD_LENGTH, "9");
		result = validator.validate(givenData, null);
		assertEquals("[Tarkastelujakson pituus on oltava väliltä 10-100]", result.listErrors().toString());

		givenData.getModel().removeAll(new Predicate(Evaluation.EVALUATION_PERIOD_LENGTH));
		givenData.getModel().addStatementIfObjectGiven(Evaluation.EVALUATION_PERIOD_LENGTH, "101");
		result = validator.validate(givenData, null);
		assertEquals("[Tarkastelujakson pituus on oltava väliltä 10-100]", result.listErrors().toString());
	}

	@Test
	public void dd_na_ne() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		Evaluation givenData = createReadyEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(Evaluation.RED_LIST_STATUS, new Qname("MX.iucnDD"));
		givenModel.addStatementIfObjectGiven(Evaluation.RED_LIST_STATUS_MIN, new Qname("MX.iucnDD"));
		givenModel.addStatementIfObjectGiven(Evaluation.RED_LIST_STATUS_MAX, new Qname("MX.iucnNE"));
		givenModel.addStatementIfObjectGiven(Evaluation.STATUS_A, new Qname("MX.iucnDD"));
		givenModel.addStatementIfObjectGiven(Evaluation.STATUS_B, new Qname("MX.iucnNA"));
		givenModel.addStatementIfObjectGiven(Evaluation.STATUS_C, new Qname("MX.iucnNE"));
		givenModel.addStatementIfObjectGiven(Evaluation.EXTERNAL_IMPACT, new Qname("MKV.externalPopulationImpactOnRedListStatusEnumMinus1"));

		// Silence some other validations
		givenModel.addStatementIfObjectGiven(Evaluation.CRITERIA_A, "A1a");
		givenModel.addStatementIfObjectGiven(Evaluation.CRITERIA_B, "B1a");
		givenModel.addStatementIfObjectGiven(Evaluation.CRITERIA_C, "C1");
		givenModel.addStatementIfObjectGiven(Evaluation.EVALUATION_PERIOD_LENGTH, "20");

		ValidationResult result = validator.validate(givenData, null);
		assertEquals(""+
				"[Luokan alennusta/korotusta ei saa käyttää luokille DD, NA, NE, RE, EW, EX, " +
				"DD syy on ilmoitettava, " + 
				"Arvoa \"DD – Puutteellisesti tunnetut\" ei voi käyttää arvovälinä, "+
				"Luokkaa \"DD – Puutteellisesti tunnetut\" ei voi käyttää kriteerin aiheuttamana luokkana, "+
				"Luokkaa \"NA – Arviointiin soveltumattomat\" ei voi käyttää kriteerin aiheuttamana luokkana, "+
				"Luokkaa \"NE – Arvioimatta jätetyt\" ei voi käyttää kriteerin aiheuttamana luokkana]", result.listErrors().toString());
	}

	@Test
	public void test_too_long_text() throws Exception {
		String longtext = "longtext is long! ";
		while (longtext.length() < 4500) {
			longtext += longtext;
		}
		
		Model givenModel = new Model(new Qname("Foo"));
		Evaluation givenData = createReadyEvaluation(givenModel);
		givenData.getModel().addStatementIfObjectGiven(Evaluation.RED_LIST_STATUS, new Qname("MX.iucnLC"));
		givenData.getModel().addStatementIfObjectGiven(Evaluation.DISTRIBUATION_AREA_NOTES, longtext);
		ValidationResult result = validator.validate(givenData, null);
		assertEquals("[Liian pitkä teksti kentässä Yksityiset muistiinpanot levinneisyysalueen koosta]", 
				result.listErrors().toString());
	}
	
}
