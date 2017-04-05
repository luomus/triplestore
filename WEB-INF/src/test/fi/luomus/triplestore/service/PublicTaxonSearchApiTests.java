package fi.luomus.triplestore.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fi.luomus.commons.config.Config;
import fi.luomus.commons.config.ConfigReader;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.reporting.ErrorReporingToSystemErr;
import fi.luomus.commons.taxonomy.TaxonomyDAO.TaxonSearch;
import fi.luomus.commons.xml.Document.Node;
import fi.luomus.triplestore.dao.DataSourceDefinition;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.dao.TriplestoreDAOConst;
import fi.luomus.triplestore.dao.TriplestoreDAOImple;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAOImple;

public class PublicTaxonSearchApiTests {

	private static TriplestoreDAO triplestoreDAO;
	private static ExtendedTaxonomyDAOImple taxonomyDAO;
	private static DataSource dataSource;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Config config = new ConfigReader("C:/apache-tomcat/app-conf/triplestore-v2-taxonomyeditor.properties");
		TriplestoreDAOConst.SCHEMA = config.get("LuontoDbName");
		dataSource = DataSourceDefinition.initDataSource(config.connectionDescription());
		triplestoreDAO = new TriplestoreDAOImple(dataSource, TriplestoreDAO.TEST_USER);
		taxonomyDAO = new ExtendedTaxonomyDAOImple(config, triplestoreDAO, new ErrorReporingToSystemErr());
	}

	@AfterClass 
	public static void tearDownAfterClass() throws Exception {
		dataSource.close();
		taxonomyDAO.close();
	}

	@Test
	public void test_exact_match() throws Exception {
		Node n = taxonomyDAO.search(new TaxonSearch("susi", 10)).getRootNode();
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
		assertEquals(2, match.getNode("informalGroups").getChildNodes().size()); // petoeläimet, nisäkkäät
	}

	@Test
	public void test_exact_search_from_null_checklist() throws Exception {
		Node n = taxonomyDAO.search(new TaxonSearch("susi", 10, null)).getRootNode();
		assertEquals(0, n.getChildNodes().size());
	}

	@Test
	public void test_exact_search_from_null_checklist_2() throws Exception {
		Node n = taxonomyDAO.search(new TaxonSearch("teStI", 10, null)).getRootNode();
		assertTrue(n.getNode("exactMatch").getChildNodes().size() > 0);
	}

	@Test
	public void test_missing_searchword() throws Exception {
		Node n = taxonomyDAO.search(new TaxonSearch(null, 10)).getRootNode();
		assertEquals("Search word must be given.", n.getAttribute("error"));
	}

	@Test
	public void test_too_short_searchword() throws Exception {
		Node n = taxonomyDAO.search(new TaxonSearch("a", 10)).getRootNode();
		assertEquals("Search word was too short.", n.getAttribute("error"));

		n = taxonomyDAO.search(new TaxonSearch("aa", 10)).getRootNode();
		assertFalse(n.hasAttribute("error"));

		n = taxonomyDAO.search(new TaxonSearch("aaaa", 10)).getRootNode();
		assertFalse(n.hasAttribute("error"));
	}

	@Test
	public void test_likely_match() throws Exception {
		Node n = taxonomyDAO.search(new TaxonSearch("susiåpus", 10)).getRootNode();
		assertEquals(1, n.getChildNodes().size());
		assertEquals(1, n.getNode("likelyMatches").getChildNodes().size());
		Node match = n.getNode("likelyMatches").getChildNodes().get(0);
		assertEquals("MX.46549", match.getName());
		assertEquals("0.889", match.getAttribute("similarity"));
	}

	@Test
	public void test_only_exact_match() throws Exception {
		Node n = taxonomyDAO.search(new TaxonSearch("susiåpus", 10).onlyExact()).getRootNode();
		assertEquals(0, n.getChildNodes().size());
	}
	
	@Test
	public void test_partial_match_unlimited() throws Exception {
		Node n = taxonomyDAO.search(new TaxonSearch("kotka", 10000)).getRootNode();
		assertTrue(n.getNode("likelyMatches").getChildNodes().size() > 3);
		assertTrue(n.getNode("partialMatches").getChildNodes().size() > 30);
	}

	@Test 
	public void test_filter_by_informal_groups() throws Exception {
		Node n = taxonomyDAO.search(
				new TaxonSearch("kotka", 10000)
				.addInformalTaxonGroup(new Qname("MVL.1")) // Linnut
				).getRootNode();
		
		assertTrue(contains("maakotka", n.getNode("partialMatches")));
		assertFalse(contains("kotkansiipi", n.getNode("partialMatches")));
	}

	@Test 
	public void test_filter_by_informal_groups_2() throws Exception {
		Node n = taxonomyDAO.search(
				new TaxonSearch("kotka", 10000)
				).getRootNode();
		
		assertTrue(contains("maakotka", n.getNode("partialMatches")));
		assertTrue(contains("kotkansiipi", n.getNode("partialMatches")));
	}
	
	@Test 
	public void test_filter_by_informal_groups_3() throws Exception {
		Node n = taxonomyDAO.search(
				new TaxonSearch("kotka", 10000)
				.addInformalTaxonGroup(new Qname("MVL.1")) // Linnut
				.addInformalTaxonGroup(new Qname("MVL.21")) // Kasvit
				).getRootNode();
		
		assertTrue(contains("maakotka", n.getNode("partialMatches")));
		assertTrue(contains("kotkansiipi", n.getNode("partialMatches")));
	}
	
	@Test 
	public void test_filter_by_informal_groups_4() throws Exception {
		Node n = taxonomyDAO.search(
				new TaxonSearch("kotka", 10000)
				.addInformalTaxonGroup(new Qname("MVL.21")) // Kasvit
				).getRootNode();
		
		assertFalse(contains("maakotka", n.getNode("partialMatches")));
		assertTrue(contains("kotkansiipi", n.getNode("partialMatches")));
	}
	
	private boolean contains(String name, Node node) {
		for (Node n : node) {
			if (n.getAttribute("matchingName").equals(name)) return true;
		}
		return false;
	}
	
}
