package fi.luomus.triplestore.taxonomy.iucn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import fi.luomus.commons.config.Config;
import fi.luomus.commons.config.ConfigReader;
import fi.luomus.commons.containers.rdf.Model;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.reporting.ErrorReporingToSystemErr;
import fi.luomus.commons.reporting.ErrorReporter;
import fi.luomus.triplestore.dao.DataSourceDefinition;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.dao.TriplestoreDAOConst;
import fi.luomus.triplestore.dao.TriplestoreDAOImple;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEndangermentObject;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluation;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNValidationResult;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNValidator;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

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
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.EVALUATED_TAXON, "MX.1", null);
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.EVALUATION_YEAR, "2000", null);
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.STATE, IUCNEvaluation.STATE_STARTED, null);
		return givenData;
	}

	private IUCNEvaluation createReadyEvaluation(Model givenModel) throws Exception {
		IUCNEvaluation givenData = createEvaluation(givenModel);
		givenModel.removeAll(new Predicate(IUCNEvaluation.STATE));
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.STATE, IUCNEvaluation.STATE_READY, null);
		return givenData;
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
		givenModel.addStatementIfObjectGiven("MKV.redListStatusMin", "MX.iucnLC", null);
		givenModel.addStatementIfObjectGiven("MKV.redListStatusMax", "MX.iucnNE", null);
		IUCNValidationResult result = validator.validate(givenData, null);
		assertTrue(result.hasErrors());
		assertEquals("Arvoa \"NE - Arvioimatta jätetyt\" ei voi käyttää arvovälinä", result.listErrors().get(0));
	}

	@Test
	public void test_min_max_iucn_range_2() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		IUCNEvaluation givenData = createEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven("MKV.redListStatusMin", "MX.iucnLC", null);
		givenModel.addStatementIfObjectGiven("MKV.redListStatusMax", "MX.iucnVU", null);
		IUCNValidationResult result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());
	}

	@Test
	public void test_min_max_iucn_range_3() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		IUCNEvaluation givenData = createEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven("MKV.redListStatusMin", "MX.iucnVU", null);
		givenModel.addStatementIfObjectGiven("MKV.redListStatusMax", "MX.iucnLC", null);
		IUCNValidationResult result = validator.validate(givenData, null);
		assertTrue(result.hasErrors());
		assertEquals("Arvovälin ala-arvo \"VU - Vaarantuneet\" ei saa olla suurempi kuin yläarvo \"LC - Elinvoimaiset\"", result.listErrors().get(0));
	}

	@Test
	public void test_endagerement_reason_1() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		IUCNEvaluation givenData = createReadyEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.RED_LIST_STATUS, "MX.iucnLC", null);

		IUCNValidationResult result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());
	}
	
	@Test
	public void test_endagerement_reason_2() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		IUCNEvaluation givenData = createReadyEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.RED_LIST_STATUS, "MX.iucnVU", null);

		IUCNValidationResult result = validator.validate(givenData, null);
		assertTrue(result.hasErrors());
		assertEquals("Uhanalaisuuden syyt on määriteltävä uhanalaisuusluokalle \"VU - Vaarantuneet\"", result.listErrors().get(0));
	}

	@Test
	public void test_endagerement_reason_3() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		IUCNEvaluation givenData = createReadyEvaluation(givenModel);
		givenModel.addStatementIfObjectGiven(IUCNEvaluation.RED_LIST_STATUS, "MX.iucnVU", null);
		givenData.addEndangermentReason(new IUCNEndangermentObject(null, new Qname("some"), 0));
		IUCNValidationResult result = validator.validate(givenData, null);
		if (result.hasErrors()) System.out.println(result.getErrors());
		assertFalse(result.hasErrors());
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

}
