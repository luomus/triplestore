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

import org.junit.BeforeClass;
import org.junit.Test;

public class PublicTaxonSearchApiTests {

	private static TriplestoreDAO triplestoreDAO;
	private static TaxonomyDAO taxonomyDAO;


	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Config config = new ConfigReader("C:/apache-tomcat/app-conf/triplestore-v2-taxonomyeditor.properties");
		TriplestoreDAOConst.SCHEMA = config.get("LuontoDbName");
		triplestoreDAO = new TriplestoreDAOImple(DataSourceDefinition.initDataSource(config.connectionDescription()), TriplestoreDAO.TEST_USER);
		taxonomyDAO = new ExtendedTaxonomyDAOImple(config, triplestoreDAO);
	}

	@Test
	public void test_exact_match() throws Exception {
		Node n = taxonomyDAO.search("susi").getRootNode();
		assertEquals(1, n.getChildNodes().size());
		assertEquals(1, n.getNode("exactMatch").getChildNodes().size());
		Node match = n.getNode("exactMatch").getChildNodes().get(0);
		assertEquals("MX.46549", match.getName());
		assertEquals("Canis lupus", match.getAttribute("scientificName"));
		assertEquals("susi", match.getContents());
		assertEquals("Linnaeus, 1758", match.getAttribute("scientificNameAuthorship"));
		assertEquals("MX.species", match.getAttribute("taxonRank"));
	}

	@Test
	public void test_exact_search_from_null_checklist() throws Exception {
		Node n = taxonomyDAO.search("susi", null).getRootNode();
		assertEquals(0, n.getChildNodes().size());
	}

	@Test
	public void test_exact_search_from_null_checklist_2() throws Exception {
		Node n = taxonomyDAO.search("teStI", null).getRootNode();
		assertTrue(n.getNode("exactMatch").getChildNodes().size() > 0);
	}

	@Test
	public void test_missing_searchword() throws Exception {
		Node n = taxonomyDAO.search(null, null).getRootNode();
		assertEquals("Search word must be given.", n.getAttribute("error"));
	}

	@Test
	public void test_too_short_searchword() throws Exception {
		Node n = taxonomyDAO.search("a", null).getRootNode();
		assertEquals("Search word was too short.", n.getAttribute("error"));

		n = taxonomyDAO.search("aa", null).getRootNode();
		assertFalse(n.hasAttribute("error"));

		n = taxonomyDAO.search("aaaa", null).getRootNode();
		assertFalse(n.hasAttribute("error"));
	}

	@Test
	public void test_likely_match() throws Exception {
		Node n = taxonomyDAO.search("susiåpus").getRootNode();
		assertEquals(1, n.getChildNodes().size());
		assertEquals(1, n.getNode("likelyMatches").getChildNodes().size());
		Node match = n.getNode("likelyMatches").getChildNodes().get(0);
		assertEquals("MX.46549", match.getName());
		assertEquals("0.889", match.getAttribute("similarity"));
	}

	@Test
	public void test_partial_match() throws Exception {
		Node n = taxonomyDAO.search("kotka").getRootNode();
		assertTrue(n.getNode("likelyMatches").getChildNodes().size() > 3);
		assertTrue(n.getNode("partialMatches").getChildNodes().size() > 30);
	}


}
