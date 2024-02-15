package fi.luomus.triplestore.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zaxxer.hikari.HikariDataSource;

import fi.luomus.commons.config.Config;
import fi.luomus.commons.config.ConfigReader;
import fi.luomus.commons.reporting.ErrorReportingToSystemErr;
import fi.luomus.commons.xml.Document.Node;
import fi.luomus.commons.xml.XMLReader;
import fi.luomus.triplestore.dao.DataSourceDefinition;
import fi.luomus.triplestore.dao.SearchParams;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.dao.TriplestoreDAOConst;
import fi.luomus.triplestore.dao.TriplestoreDAOImple;
import fi.luomus.triplestore.service.EditorBaseServlet.Format;

public class SearchServiceTests {

	private static TriplestoreDAO dao;
	private static HikariDataSource dataSource;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Config config = new ConfigReader("C:/apache-tomcat/app-conf/triplestore-v2.properties");
		TriplestoreDAOConst.SCHEMA = config.get("LuontoDbName");
		dataSource = DataSourceDefinition.initDataSource(config.connectionDescription());
		dao = new TriplestoreDAOImple(dataSource, TriplestoreDAO.TEST_USER, new ErrorReportingToSystemErr());
	}

	@AfterClass
	public static void tearDownAfterClass() {
		dataSource.close();
	}

	@Test
	public void test_search_predicate_object() throws Exception {
		SearchParams searchParams = new SearchParams(SearchServlet.DEFAULT_MAX_RESULT_COUNT, 0).predicate("MX.isPartOf").object("MX.34");
		String response = SearchServlet.search(searchParams, SearchServlet.DEFAULT_FORMAT, dao);
		assertTrue(response.contains("<MX.taxon rdf:about=\"http://tun.fi/MX.37\">"));
		assertTrue(response.contains("<MX.taxon rdf:about=\"http://tun.fi/MX.35\">"));
	}

	@Test
	public void test_search_predicate_objectresource() throws Exception {
		SearchParams searchParams = new SearchParams(SearchServlet.DEFAULT_MAX_RESULT_COUNT, 0).predicate("MX.isPartOf").objectresource("MX.34");
		String response = SearchServlet.search(searchParams, SearchServlet.DEFAULT_FORMAT, dao);
		assertTrue(response.contains("<MX.taxon rdf:about=\"http://tun.fi/MX.37\">"));
		assertTrue(response.contains("<MX.taxon rdf:about=\"http://tun.fi/MX.35\">"));
	}

	@Test
	public void test_search_predicate_objectliteral() throws Exception {
		SearchParams searchParams = new SearchParams(SearchServlet.DEFAULT_MAX_RESULT_COUNT, 0).predicate("MX.scientificName").objectliteral("Biota");
		String response = SearchServlet.search(searchParams, SearchServlet.DEFAULT_FORMAT, dao);
		assertTrue(response.contains("<MX.taxon rdf:about=\"http://tun.fi/MX.37600\">"));
	}

	@Test
	public void test_search_predicate_object_where_object_is_literal() throws Exception {
		SearchParams searchParams = new SearchParams(SearchServlet.DEFAULT_MAX_RESULT_COUNT, 0).predicate("MX.scientificName").object("Biota");
		String response = SearchServlet.search(searchParams, SearchServlet.DEFAULT_FORMAT, dao);
		assertTrue(response.contains("<MX.taxon rdf:about=\"http://tun.fi/MX.37600\">"));
	}

	@Test
	public void test_search_predicate__multi_objects() throws Exception {
		SearchParams searchParams = new SearchParams(SearchServlet.DEFAULT_MAX_RESULT_COUNT, 0).predicate("MX.scientificName").object("Plantae").object("Animalia");
		String response = SearchServlet.search(searchParams, SearchServlet.DEFAULT_FORMAT, dao);
		assertTrue(response.contains("<MX.taxon rdf:about=\"http://tun.fi/MX.37601\">"));
		assertTrue(response.contains("<MX.taxon rdf:about=\"http://tun.fi/MX.37602\">"));
	}

	@Test
	public void test_search_predicate__multi_subjects() throws Exception {
		SearchParams searchParams = new SearchParams(SearchServlet.DEFAULT_MAX_RESULT_COUNT, 0).subject("MX.37601").subject("MX.37602");
		String response = SearchServlet.search(searchParams, SearchServlet.DEFAULT_FORMAT, dao);
		assertTrue(response.contains("<MX.taxon rdf:about=\"http://tun.fi/MX.37601\">"));
		assertTrue(response.contains("<MX.taxon rdf:about=\"http://tun.fi/MX.37602\">"));
		Node n = new XMLReader().parse(response).getRootNode();
		assertEquals(2, n.getChildNodes("MX.taxon").size());
	}

	@Test
	public void test_search_default_limit_and_offset() throws Exception {
		SearchParams searchParams = new SearchParams(SearchServlet.DEFAULT_MAX_RESULT_COUNT, 0).predicate("rdf:type").objectresource("MA.person");
		String response = SearchServlet.search(searchParams, Format.RDFXML, dao);
		Node n = new XMLReader().parse(response).getRootNode();
		assertEquals(SearchServlet.DEFAULT_MAX_RESULT_COUNT, n.getChildNodes().size());
	}

	@Test
	public void test_search_limit_and_offset() throws Exception {
		SearchParams searchParams = new SearchParams(10, 0).predicate("rdf:type").objectresource("MA.person");
		String response = SearchServlet.search(searchParams, Format.RDFXML, dao);
		Node n = new XMLReader().parse(response).getRootNode();
		assertEquals(10, n.getChildNodes().size());
	}

	@Test
	public void test_search_limit_and_offset_2() throws Exception {
		SearchParams searchParams = new SearchParams(10, 0).type("MA.person");
		String response = SearchServlet.search(searchParams, Format.RDFXML, dao);

		Node n = new XMLReader().parse(response).getRootNode();
		assertEquals(10, n.getChildNodes().size());
		int i = 1;
		for (Node person : n.getChildNodes()) {
			int id = Integer.valueOf(person.getAttribute("rdf:about").replace("http://tun.fi/MA.", ""));
			assertEquals(i++, id);
		}
	}

	@Test
	public void test_search_limit_and_offset_3() throws Exception {
		SearchParams searchParams = new SearchParams(10, 5).type("MA.person");
		String response = SearchServlet.search(searchParams, Format.RDFXML, dao);

		Node n = new XMLReader().parse(response).getRootNode();
		assertEquals(10, n.getChildNodes().size());
		int i = 6;
		for (Node person : n.getChildNodes()) {
			int id = Integer.valueOf(person.getAttribute("rdf:about").replace("http://tun.fi/MA.", ""));
			assertEquals(i++, id);
		}
	}

	@Test
	public void test_search_by_type() throws Exception {
		String normalResponse = withoutTypeParameter();
		String typeParameterResponse = withTypeParameter();
		assertEquals(normalResponse, typeParameterResponse);
	}

	private String withTypeParameter() throws Exception {
		SearchParams searchParams = new SearchParams(5, 0).type("MA.person");
		String response = SearchServlet.search(searchParams, Format.RDFXML, dao);
		return response;
	}

	private String withoutTypeParameter() throws Exception {
		SearchParams searchParams = new SearchParams(5, 0).predicate("rdf:type").objectresource("MA.person");
		String response = SearchServlet.search(searchParams, Format.RDFXML, dao);
		return response;
	}

	@Test
	public void test_search_no_matches() throws Exception {
		SearchParams searchParams = new SearchParams(10, 5).predicate("blablala").objectresource("blabalba");
		String response = SearchServlet.search(searchParams, Format.RDFXML, dao);
		Node root = new XMLReader().parse(response).getRootNode();
		assertFalse(root.hasChildNodes());
	}

}
