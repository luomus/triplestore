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
import fi.luomus.triplestore.dao.DataSourceDefinition;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.dao.TriplestoreDAOConst;
import fi.luomus.triplestore.dao.TriplestoreDAOImple;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEndangermentObject;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluation;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNHabitatObject;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNValidationResult;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNValidator;

public class IUCNValidatorTests {

	private static TriplestoreDAO dao;
	private static DataSource dataSource;
	private static IUCNValidator validator;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Config config = new ConfigReader("C:/apache-tomcat/app-conf/triplestore-v2.properties");
		TriplestoreDAOConst.SCHEMA = config.get("LuontoDbName");
		dataSource = DataSourceDefinition.initDataSource(config.connectionDescription());
		dao = new TriplestoreDAOImple(dataSource, TriplestoreDAO.TEST_USER);
		ErrorReporter errorReporter = new ErrorReporingToSystemErr();
		validator = new IUCNValidator(dao, errorReporter);
	}

	@AfterClass
	public static void afterClass() throws Exception {
		dataSource.close();
	}

	@Test
	public void notInitialized() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		IUCNEvaluation givenData = new IUCNEvaluation(givenModel, dao.getProperties(IUCNEvaluation.EVALUATION_CLASS));

		IUCNValidationResult result = validator.validate(givenData, null);
		assertTrue(result.hasErrors());
		assertEquals(
				"[Ohjelmointivirhe: rdf:type puuttuu!, Ohjelmointivirhe: MKV.evaluatedTaxon puuttuu!, Ohjelmointivirhe: MKV.evaluationYear puuttuu!, Ohjelmointivirhe: MKV.state puuttuu!]", 
				result.listErrors().toString());
	}

	@Test
	public void correctlyInitialized() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		IUCNEvaluation givenData = createEvaluation(givenModel);

		IUCNValidationResult result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());
	}

	private IUCNEvaluation createEvaluation(Model givenModel) throws Exception {
		IUCNEvaluation givenData = new IUCNEvaluation(givenModel, dao.getProperties(IUCNEvaluation.EVALUATION_CLASS));
		givenModel.setType(IUCNEvaluation.EVALUATION_CLASS);
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.EVALUATED_TAXON, new Qname("MX.1"));
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.EVALUATION_YEAR, "2000");
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.STATE, new Qname(IUCNEvaluation.STATE_STARTED));
		return givenData;
	}

	private IUCNEvaluation createReadyEvaluation(Model givenModel) throws Exception {
		IUCNEvaluation givenData = createEvaluation(givenModel);
		givenModel.removeAll(new Predicate(IUCNEvaluation.STATE));
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.STATE, new Qname(IUCNEvaluation.STATE_READY));
		givenData.setPrimaryHabitat(new IUCNHabitatObject(null, new Qname("Something required"), 1));
		return givenData;
	}

	@Test
	public void invalidDataTypes() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		IUCNEvaluation givenData = createEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.STATE, "literal");
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.DISTRIBUTION_AREA_MAX, "alpha");
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.RED_LIST_STATUS, new Qname("MX.invalidEnum"));
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.GENERATION_AGE, "6,5");
		IUCNValidationResult result = validator.validate(givenData, null);
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
		IUCNEvaluation givenData = createReadyEvaluation(givenModel);

		IUCNValidationResult result = validator.validate(givenData, null);
		assertTrue(result.hasErrors());
		assertEquals(
				"[Pakollinen tieto: Luokka]", 
				result.listErrors().toString());
	}

	@Test
	public void test_min_max_integer() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		IUCNEvaluation givenData = createEvaluation(givenModel);

		givenModel.addStatementIfObjectGiven(IUCNEvaluation.OCCURRENCE_AREA_MIN, "4", null);
		IUCNValidationResult result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());

		givenModel.addStatementIfObjectGiven(IUCNEvaluation.OCCURRENCE_AREA_MAX, "4", null);
		result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());

		givenModel.removeAll(new Predicate(IUCNEvaluation.OCCURRENCE_AREA_MAX));
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.OCCURRENCE_AREA_MAX, "5", null);
		result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());

		givenModel.removeAll(new Predicate(IUCNEvaluation.OCCURRENCE_AREA_MAX));
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.OCCURRENCE_AREA_MAX, "3", null);
		result = validator.validate(givenData, null);
		assertTrue(result.hasErrors());
		assertEquals("Arvovälin ala-arvo 4 ei saa olla suurempi kuin yläarvo 3", result.listErrors().get(0));
	}

	@Test
	public void test_min_max_iucn_range() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		IUCNEvaluation givenData = createEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.RED_LIST_STATUS_MIN, new Qname("MX.iucnLC"));
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.RED_LIST_STATUS_MAX, new Qname("MX.iucnNE"));
		IUCNValidationResult result = validator.validate(givenData, null);
		assertTrue(result.hasErrors());
		assertEquals("Arvoa \"NE - Arvioimatta jätetyt\" ei voi käyttää arvovälinä", result.listErrors().get(0));
	}

	@Test
	public void test_min_max_iucn_range_2() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		IUCNEvaluation givenData = createEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.RED_LIST_STATUS_MIN, new Qname("MX.iucnLC"));
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.RED_LIST_STATUS_MAX, new Qname("MX.iucnVU"));
		IUCNValidationResult result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());
	}

	@Test
	public void test_min_max_iucn_range_3() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		IUCNEvaluation givenData = createEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.RED_LIST_STATUS_MIN, new Qname("MX.iucnVU"));
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.RED_LIST_STATUS_MAX, new Qname("MX.iucnLC"));
		IUCNValidationResult result = validator.validate(givenData, null);
		assertTrue(result.hasErrors());
		assertEquals("Arvovälin ala-arvo \"VU - Vaarantuneet\" ei saa olla suurempi kuin yläarvo \"LC - Elinvoimaiset\"", result.listErrors().get(0));
	}

	@Test
	public void test_invasive() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		IUCNEvaluation givenData = createReadyEvaluation(givenModel);

		givenModel.addStatementIfObjectGiven(IUCNEvaluation.TYPE_OF_OCCURRENCE_IN_FINLAND, new Qname("MX.typeOfOccurrenceAnthropogenic"));
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.RED_LIST_STATUS, new Qname("MX.iucnLC"));

		IUCNValidationResult result = validator.validate(givenData, null);
		assertTrue(result.hasErrors());
		assertEquals("Vieraslajille ainut sallittu luokka on NA", result.listErrors().get(0));
	}

	@Test
	public void test_invasive_2() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		IUCNEvaluation givenData = createReadyEvaluation(givenModel);

		givenModel.addStatementIfObjectGiven(IUCNEvaluation.TYPE_OF_OCCURRENCE_IN_FINLAND, new Qname("MX.typeOfOccurrenceAnthropogenic"));
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.RED_LIST_STATUS, new Qname("MX.iucnNA"));

		IUCNValidationResult result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());
	}

	@Test
	public void test_non_invasive() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		IUCNEvaluation givenData = createReadyEvaluation(givenModel);

		givenModel.addStatementIfObjectGiven(IUCNEvaluation.TYPE_OF_OCCURRENCE_IN_FINLAND, new Qname("MX.typeOfOccurrenceOccurs"));
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.RED_LIST_STATUS, new Qname("MX.iucnLC"));

		IUCNValidationResult result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());
	}

	@Test
	public void test_criteria_and_status_1() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		IUCNEvaluation givenData = createReadyEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.RED_LIST_STATUS, new Qname("MX.iucnLC"));

		givenModel.addStatementIfObjectGiven(IUCNEvaluation.CRITERIA_B, "B2b(ii,iii,v)c(iv)");
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.STATUS_B, new Qname("MX.iucnNT"));

		IUCNValidationResult result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());
	}

	@Test
	public void test_criteria_and_status_2() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		IUCNEvaluation givenData = createReadyEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.RED_LIST_STATUS, new Qname("MX.iucnLC"));

		givenModel.addStatementIfObjectGiven(IUCNEvaluation.CRITERIA_B, "B2b(ii,iii,v)c(iv)");

		IUCNValidationResult result = validator.validate(givenData, null);
		assertEquals("[Kriteeri B ja siitä seuraava luokka on annettava jos toinen tiedoista annetaan]", result.listErrors().toString());
	}

	@Test
	public void test_criteria_and_status_3() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		IUCNEvaluation givenData = createReadyEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.RED_LIST_STATUS, new Qname("MX.iucnLC"));

		givenModel.addStatementIfObjectGiven(IUCNEvaluation.STATUS_C, new Qname("MX.iucnNT"));

		IUCNValidationResult result = validator.validate(givenData, null);
		assertEquals("[Kriteeri C ja siitä seuraava luokka on annettava jos toinen tiedoista annetaan]", result.listErrors().toString());
	}


	@Test
	public void test_criteria_B1() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		IUCNEvaluation givenData = createReadyEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.RED_LIST_STATUS, new Qname("MX.iucnLC"));

		givenModel.addStatementIfObjectGiven(IUCNEvaluation.DISTRIBUTION_AREA_MIN, "4");
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.CRITERIA_FOR_STATUS, "B1a");

		IUCNValidationResult result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());
	}

	@Test
	public void test_criteria_B1_2() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		IUCNEvaluation givenData = createReadyEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.RED_LIST_STATUS, new Qname("MX.iucnLC"));

		givenModel.addStatementIfObjectGiven(IUCNEvaluation.DISTRIBUTION_AREA_MIN, "4");
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.CRITERIA_B, "B1a");
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.STATUS_B, new Qname("MX.iucnVU"));

		IUCNValidationResult result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());
	}

	@Test
	public void test_criteria_B1_3() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		IUCNEvaluation givenData = createReadyEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.RED_LIST_STATUS, new Qname("MX.iucnLC"));

		givenModel.addStatementIfObjectGiven(IUCNEvaluation.DISTRIBUTION_AREA_MIN, "4");

		IUCNValidationResult result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());
	}

	public void test_criteria_B1_4() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		IUCNEvaluation givenData = createReadyEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.RED_LIST_STATUS, new Qname("MX.iucnLC"));

		givenModel.addStatementIfObjectGiven(IUCNEvaluation.CRITERIA_B, "B1a");

		IUCNValidationResult result = validator.validate(givenData, null);
		assertEquals("Levinneisyysalueen koko on ilmoitettava käytettäessä kriteeriä B1", result.listErrors().toString());
	}

	public void test_criteria_B1_5() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		IUCNEvaluation givenData = createReadyEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.RED_LIST_STATUS, new Qname("MX.iucnLC"));

		givenModel.addStatementIfObjectGiven(IUCNEvaluation.CRITERIA_FOR_STATUS, "B1a");

		IUCNValidationResult result = validator.validate(givenData, null);
		assertEquals("Levinneisyysalueen koko on ilmoitettava käytettäessä kriteeriä B1", result.listErrors().toString());
	}

	@Test
	public void test_criteria_various() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		IUCNEvaluation givenData = createReadyEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.RED_LIST_STATUS, new Qname("MX.iucnLC"));

		givenModel.addStatementIfObjectGiven(IUCNEvaluation.CRITERIA_FOR_STATUS, "A1a; B1a+2a");

		IUCNValidationResult result = validator.validate(givenData, null);
		assertEquals("" +
				"[Levinneisyysalueen koko on ilmoitettava käytettäessä kriteeriä B1, "+
				"Esiintymisalueen koko on ilmoitettava käytettäessä kriteeriä B2, "+
				"Tarkastelujakson pituus on ilmoitettava käytettäessä kriteeriä A]", 
				result.listErrors().toString());
	}

	@Test
	public void test_occurrences_primHabitat_threats_endangerment_1() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		IUCNEvaluation givenData = createReadyEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.RED_LIST_STATUS, new Qname("MX.iucnVU"));
		givenData.setPrimaryHabitat(null);

		IUCNValidationResult result = validator.validate(givenData, null);
		assertEquals("" +
				"[Esiintymisalueet on täytettävä luokille NT-CR, "+
				"Ensisijainen elinympäristö on täytettävä luokille LC-CR, "+
				"Uhanalaisuuden syyt on täytettävä luokille VU-RE, "+
				"Uhkatekijät on täytettävä luokille VU-CR, "+
				"Luokkaan johtaneet kriteerit on täytettävä luokille NT-CR]",
				result.listErrors().toString());
	}

	@Test
	public void test_occurrences_primHabitat_threats_endangerment_2() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		IUCNEvaluation givenData = createReadyEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.RED_LIST_STATUS, new Qname("MX.iucnVU"));
		givenData.addOccurrence(new Occurrence(null, new Qname("some"), new Qname("occ")));
		givenData.addEndangermentReason(new IUCNEndangermentObject(null, new Qname("some"), 0));
		givenData.addThreat(new IUCNEndangermentObject(null, new Qname("some"), 0));
		givenData.getModel().addStatementIfObjectGiven(IUCNEvaluation.CRITERIA_FOR_STATUS, "E");
		IUCNValidationResult result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());
	}

	@Test
	public void test_evaluationPeriodLength() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		IUCNEvaluation givenData = createEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.RED_LIST_STATUS, new Qname("MX.iucnLC"));
		givenData.getModel().addStatementIfObjectGiven(IUCNEvaluation.EVALUATION_PERIOD_LENGTH, "10");
		IUCNValidationResult result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());

		givenData.getModel().removeAll(new Predicate(IUCNEvaluation.EVALUATION_PERIOD_LENGTH));
		givenData.getModel().addStatementIfObjectGiven(IUCNEvaluation.EVALUATION_PERIOD_LENGTH, "100");
		result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());

		givenData.getModel().removeAll(new Predicate(IUCNEvaluation.EVALUATION_PERIOD_LENGTH));
		givenData.getModel().addStatementIfObjectGiven(IUCNEvaluation.EVALUATION_PERIOD_LENGTH, "50");
		result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());

		givenData.getModel().removeAll(new Predicate(IUCNEvaluation.EVALUATION_PERIOD_LENGTH));
		givenData.getModel().addStatementIfObjectGiven(IUCNEvaluation.EVALUATION_PERIOD_LENGTH, "9");
		result = validator.validate(givenData, null);
		assertEquals("[Tarkastelujakson pituus on oltava väliltä 10-100]", result.listErrors().toString());

		givenData.getModel().removeAll(new Predicate(IUCNEvaluation.EVALUATION_PERIOD_LENGTH));
		givenData.getModel().addStatementIfObjectGiven(IUCNEvaluation.EVALUATION_PERIOD_LENGTH, "101");
		result = validator.validate(givenData, null);
		assertEquals("[Tarkastelujakson pituus on oltava väliltä 10-100]", result.listErrors().toString());
	}

	@Test
	public void dd_na_ne() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		IUCNEvaluation givenData = createReadyEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.RED_LIST_STATUS, new Qname("MX.iucnDD"));
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.RED_LIST_STATUS_MIN, new Qname("MX.iucnDD"));
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.RED_LIST_STATUS_MAX, new Qname("MX.iucnNE"));
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.STATUS_A, new Qname("MX.iucnDD"));
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.STATUS_B, new Qname("MX.iucnNA"));
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.STATUS_C, new Qname("MX.iucnNE"));

		// Silence some other validations
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.CRITERIA_A, "A1a");
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.CRITERIA_B, "B1a");
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.CRITERIA_C, "C1");
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.EVALUATION_PERIOD_LENGTH, "20");

		IUCNValidationResult result = validator.validate(givenData, null);
		assertEquals(""+
				"[Arvoa \"DD - Puuttellisesti tunnetut\" ei voi käyttää arvovälinä, "+
				"Luokkaa \"DD - Puuttellisesti tunnetut\" ei voi käyttää kriteerin aiheuttamana luokkana, "+
				"Luokkaa \"NA - Arviointiin soveltumattomat\" ei voi käyttää kriteerin aiheuttamana luokkana, "+
				"Luokkaa \"NE - Arvioimatta jätetyt\" ei voi käyttää kriteerin aiheuttamana luokkana]", result.listErrors().toString());
	}

}
