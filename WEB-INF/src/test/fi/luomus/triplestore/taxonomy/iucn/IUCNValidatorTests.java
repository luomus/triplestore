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
import fi.luomus.triplestore.dao.DataSourceDefinition;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.dao.TriplestoreDAOConst;
import fi.luomus.triplestore.dao.TriplestoreDAOImple;
import fi.luomus.triplestore.taxonomy.iucn.model.IUCNEvaluation;
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
	public void test_min_max_integer() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		IUCNEvaluation givenData = new IUCNEvaluation(givenModel, dao.getProperties(IUCNEvaluation.EVALUATION_CLASS));
		IUCNValidationResult result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());
		
		givenModel.addStamentIfObjectGiven("MKV.countOfOccurrencesMin", "4", null);
		result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());
		
		givenModel.addStamentIfObjectGiven("MKV.countOfOccurrencesMax", "4", null);
		result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());
		
		givenModel.removeAll(new Predicate("MKV.countOfOccurrencesMax"));
		givenModel.addStamentIfObjectGiven("MKV.countOfOccurrencesMax", "5", null);
		result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());
		
		givenModel.removeAll(new Predicate("MKV.countOfOccurrencesMax"));
		givenModel.addStamentIfObjectGiven("MKV.countOfOccurrencesMax", "3", null);
		result = validator.validate(givenData, null);
		assertTrue(result.hasErrors());
		assertEquals("Arvovälin ala-arvo 4 ei saa olla suurempi kuin yläarvo 3", result.listErrors().get(0));
	}

	@Test
	public void test_min_max_iucn_range() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		IUCNEvaluation givenData = new IUCNEvaluation(givenModel, dao.getProperties(IUCNEvaluation.EVALUATION_CLASS));
		IUCNValidationResult result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());
		
		givenModel.addStamentIfObjectGiven("MKV.redListStatusMin", "MX.iucnLC", null);
		givenModel.addStamentIfObjectGiven("MKV.redListStatusMax", "MX.iucnNE", null);
		result = validator.validate(givenData, null);
		assertTrue(result.hasErrors());
		assertEquals("Arvoa \"NE - Arvioimatta jätetty\" ei voi käyttää arvovälinä", result.listErrors().get(0));
	}
	
	@Test
	public void test_min_max_iucn_range_2() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		IUCNEvaluation givenData = new IUCNEvaluation(givenModel, dao.getProperties(IUCNEvaluation.EVALUATION_CLASS));
		IUCNValidationResult result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());
		
		givenModel.addStamentIfObjectGiven("MKV.redListStatusMin", "MX.iucnLC", null);
		givenModel.addStamentIfObjectGiven("MKV.redListStatusMax", "MX.iucnVU", null);
		result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());
	}
	
	@Test
	public void test_min_max_iucn_range_3() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		IUCNEvaluation givenData = new IUCNEvaluation(givenModel, dao.getProperties(IUCNEvaluation.EVALUATION_CLASS));
		IUCNValidationResult result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());
		
		givenModel.addStamentIfObjectGiven("MKV.redListStatusMin", "MX.iucnVU", null);
		givenModel.addStamentIfObjectGiven("MKV.redListStatusMax", "MX.iucnLC", null);
		result = validator.validate(givenData, null);
		assertTrue(result.hasErrors());
		assertEquals("Arvovälin ala-arvo \"VU - Vaarantunut\" ei saa olla suurempi kuin yläarvo \"LC - Elinvoimainen\"", result.listErrors().get(0));
	}
	
	@Test
	public void test_endagerement_reason_3() throws Exception {
		Model givenModel = new Model(new Qname("Foo"));
		givenModel.setType(IUCNEvaluation.EVALUATION_CLASS);
		givenModel.addStamentIfObjectGiven(IUCNEvaluation.RED_LIST_STATUS, "MX.iucnVU", null);
		givenModel.addStamentIfObjectGiven(IUCNEvaluation.EVALUATED_TAXON, "MX.1", null);
		givenModel.addStamentIfObjectGiven(IUCNEvaluation.EVALUATION_YEAR, "2000", null);
		IUCNEvaluation givenData = new IUCNEvaluation(givenModel, dao.getProperties(IUCNEvaluation.EVALUATION_CLASS));
		IUCNValidationResult result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());
		
		givenModel.addStamentIfObjectGiven(IUCNEvaluation.STATE, IUCNEvaluation.STATE_READY, null);
		result = validator.validate(givenData, null);
		assertTrue(result.hasErrors());
		assertEquals("Uhkatekijät on määriteltävä uhanalaisuusluokalle \"VU - Vaarantunut\"", result.listErrors().get(0));
		
		givenModel.addStamentIfObjectGiven("MKV.endangermentReason", "MKV.endangermentReasonR", null);
		result = validator.validate(givenData, null);
		assertFalse(result.hasErrors());
	}
	
}
