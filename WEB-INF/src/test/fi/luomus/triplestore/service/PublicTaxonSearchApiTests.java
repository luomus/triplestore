package fi.luomus.triplestore.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import fi.luomus.commons.config.Config;
import fi.luomus.commons.config.ConfigReader;
import fi.luomus.commons.taxonomy.TaxonomyDAO;
import fi.luomus.commons.xml.Document.Node;
import fi.luomus.lajitietokeskus.taxonomy.dao.ExtendedTaxonomyDAOImple;
import fi.luomus.triplestore.dao.DataSourceDefinition;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.dao.TriplestoreDAOConst;
import fi.luomus.triplestore.dao.TriplestoreDAOImple;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class PublicTaxonSearchApiTests {

	private static TriplestoreDAO triplestoreDAO;
	private static TaxonomyDAO taxonomyDAO;
	private static DataSource dataSource;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Config config = new ConfigReader("C:/apache-tomcat/app-conf/triplestore-v2-taxonomyeditor.properties");
		TriplestoreDAOConst.SCHEMA = config.get("LuontoDbName");
		dataSource = DataSourceDefinition.initDataSource(config.connectionDescription());
		triplestoreDAO = new TriplestoreDAOImple(dataSource, TriplestoreDAO.TEST_USER);
		taxonomyDAO = new ExtendedTaxonomyDAOImple(config, triplestoreDAO);
	}

	@AfterClass 
	public static void tearDownAfterClass() throws Exception {
		dataSource.close();
	}

	@Test
	public void test_exact_match() throws Exception {
		Node n = taxonomyDAO.search("susi", 10).getRootNode();
		System.out.println(n);
//		assertEquals(1, n.getChildNodes().size()); nyt ollaan taas silleen, että exact matchilla palautetaan muitankin osumia..
		assertEquals(1, n.getNode("exactMatch").getChildNodes().size());
		Node match = n.getNode("exactMatch").getChildNodes().get(0);
		assertEquals("MX.46549", match.getName());
		assertEquals("Canis lupus", match.getAttribute("scientificName"));
		assertEquals("susi", match.getAttribute("matchingName"));
		assertEquals("Linnaeus, 1758", match.getAttribute("scientificNameAuthorship"));
		assertEquals("MX.species", match.getAttribute("taxonRank"));
		assertEquals(1, match.getChildNodes("informalGroups").size());
		assertEquals(1, match.getNode("informalGroups").getChildNodes().size());
		assertEquals("MVL.2", match.getNode("informalGroups").getChildNodes().get(0).getName());
		assertEquals("Nisäkkäät", match.getNode("informalGroups").getChildNodes().get(0).getAttribute("fi"));
	}

	@Test
	public void test_exact_search_from_null_checklist() throws Exception {
		Node n = taxonomyDAO.search("susi", null, 10).getRootNode();
		assertEquals(0, n.getChildNodes().size());
	}

	@Test
	public void test_exact_search_from_null_checklist_2() throws Exception {
		Node n = taxonomyDAO.search("teStI", null, 10).getRootNode();
		assertTrue(n.getNode("exactMatch").getChildNodes().size() > 0);
	}

	@Test
	public void test_missing_searchword() throws Exception {
		Node n = taxonomyDAO.search(null, null, 10).getRootNode();
		assertEquals("Search word must be given.", n.getAttribute("error"));
	}

	@Test
	public void test_too_short_searchword() throws Exception {
		Node n = taxonomyDAO.search("a", null, 10).getRootNode();
		assertEquals("Search word was too short.", n.getAttribute("error"));

		n = taxonomyDAO.search("aa", null, 10).getRootNode();
		assertFalse(n.hasAttribute("error"));

		n = taxonomyDAO.search("aaaa", null, 10).getRootNode();
		assertFalse(n.hasAttribute("error"));
	}

	@Test
	public void test_likely_match() throws Exception {
		Node n = taxonomyDAO.search("susiåpus", 10).getRootNode();
		assertEquals(1, n.getChildNodes().size());
		assertEquals(1, n.getNode("likelyMatches").getChildNodes().size());
		Node match = n.getNode("likelyMatches").getChildNodes().get(0);
		assertEquals("MX.46549", match.getName());
		assertEquals("0.889", match.getAttribute("similarity"));
	}

	@Test
	public void test_partial_match_unlimited() throws Exception {
		Node n = taxonomyDAO.search("kotka", 10000).getRootNode();
		assertTrue(n.getNode("likelyMatches").getChildNodes().size() > 3);
		assertTrue(n.getNode("partialMatches").getChildNodes().size() > 30);
	}

}
