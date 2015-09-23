package fi.luomus.triplestore.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import fi.luomus.commons.config.Config;
import fi.luomus.commons.config.ConfigReader;
import fi.luomus.commons.utils.Utils;
import fi.luomus.commons.xml.Document.Node;
import fi.luomus.commons.xml.XMLReader;
import fi.luomus.triplestore.dao.DataSourceDefinition;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.dao.TriplestoreDAOConst;
import fi.luomus.triplestore.dao.TriplestoreDAOImple;
import fi.luomus.triplestore.service.EditorBaseServlet.Format;

import org.junit.BeforeClass;
import org.junit.Test;

public class SearchServiceTests {

	private static TriplestoreDAO dao;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Config config = new ConfigReader("C:/apache-tomcat/app-conf/triplestore-v2.properties");
		TriplestoreDAOConst.SCHEMA = config.get("LuontoDbName");
		dao = new TriplestoreDAOImple(DataSourceDefinition.initDataSource(config.connectionDescription()), TriplestoreDAO.TEST_USER);
	}

	@Test
	public void test_search_predicate_object() throws Exception {
		String[] subjects = null;
		String[] predicates = { "MX.isPartOf" };
		String[] objects = { "MX.34" };
		String[] objectresources = null;
		String[] objectliterals = null;
		int limit = SearchServlet.DEFAULT_MAX_RESULT_COUNT;
		int offset = 0;
		Format format = SearchServlet.DEFAULT_FORMAT;

		String response = SearchServlet.search(subjects, predicates, objects, objectresources, objectliterals, limit, offset, format, dao);

		assertTrue(response.contains("<dwc:Taxon rdf:about=\"http://id.luomus.fi/MX.37\">"));
		assertTrue(response.contains("<dwc:Taxon rdf:about=\"http://id.luomus.fi/MX.35\">"));
	}

	@Test
	public void test_search_predicate_objectresource() throws Exception {
		String[] subjects = null;
		String[] predicates = { "MX.isPartOf" };
		String[] objects = null;
		String[] objectresources = { "MX.34" };
		String[] objectliterals = null;
		int limit = SearchServlet.DEFAULT_MAX_RESULT_COUNT;
		int offset = 0;
		Format format = SearchServlet.DEFAULT_FORMAT;

		String response = SearchServlet.search(subjects, predicates, objects, objectresources, objectliterals, limit, offset, format, dao);

		assertTrue(response.contains("<dwc:Taxon rdf:about=\"http://id.luomus.fi/MX.37\">"));
		assertTrue(response.contains("<dwc:Taxon rdf:about=\"http://id.luomus.fi/MX.35\">"));
	}

	@Test
	public void test_search_predicate_objectliteral() throws Exception {
		String[] subjects = null;
		String[] predicates = { "dwc:scientificName" };
		String[] objects = null;
		String[] objectresources = null;
		String[] objectliterals = { "Biota" };
		int limit = SearchServlet.DEFAULT_MAX_RESULT_COUNT;
		int offset = 0;
		Format format = SearchServlet.DEFAULT_FORMAT;

		String response = SearchServlet.search(subjects, predicates, objects, objectresources, objectliterals, limit, offset, format, dao);

		assertTrue(response.contains("<dwc:Taxon rdf:about=\"http://id.luomus.fi/MX.37600\">"));
	}

	@Test
	public void test_search_predicate_object_where_object_is_literal() throws Exception {
		String[] subjects = null;
		String[] predicates = { "dwc:scientificName" };
		String[] objects = { "Biota" };
		String[] objectresources = null;
		String[] objectliterals = null;
		int limit = SearchServlet.DEFAULT_MAX_RESULT_COUNT;
		int offset = 0;
		Format format = SearchServlet.DEFAULT_FORMAT;

		String response = SearchServlet.search(subjects, predicates, objects, objectresources, objectliterals, limit, offset, format, dao);

		assertTrue(response.contains("<dwc:Taxon rdf:about=\"http://id.luomus.fi/MX.37600\">"));
	}

	@Test
	public void test_search_predicate__multi_objects() throws Exception {
		String[] subjects = null;
		String[] predicates = { "dwc:scientificName" };
		String[] objects = null;
		String[] objectresources = null;
		String[] objectliterals = { "Plantae", "Animalia" };
		int limit = SearchServlet.DEFAULT_MAX_RESULT_COUNT;
		int offset = 0;
		Format format = SearchServlet.DEFAULT_FORMAT;

		String response = SearchServlet.search(subjects, predicates, objects, objectresources, objectliterals, limit, offset, format, dao);

		assertTrue(response.contains("<dwc:Taxon rdf:about=\"http://id.luomus.fi/MX.37601\">"));
		assertTrue(response.contains("<dwc:Taxon rdf:about=\"http://id.luomus.fi/MX.37602\">"));
	}

	@Test
	public void test_search_predicate__multi_subjects() throws Exception {
		String[] subjects = { "MX.37601", "MX.37602" };
		String[] predicates = null;
		String[] objects = null;
		String[] objectresources = null;
		String[] objectliterals = null;
		int limit = SearchServlet.DEFAULT_MAX_RESULT_COUNT;
		int offset = 0;
		Format format = SearchServlet.DEFAULT_FORMAT;

		String response = SearchServlet.search(subjects, predicates, objects, objectresources, objectliterals, limit, offset, format, dao);

		assertTrue(response.contains("<dwc:Taxon rdf:about=\"http://id.luomus.fi/MX.37601\">"));
		assertTrue(response.contains("<dwc:Taxon rdf:about=\"http://id.luomus.fi/MX.37602\">"));
		Node n = new XMLReader().parse(response).getRootNode();
		assertEquals(2, n.getChildNodes("dwc:Taxon").size());
	}

	@Test
	public void test_search_default_limit_and_offset() throws Exception {
		String[] subjects = null;
		String[] predicates = { "rdf:type" };
		String[] objects = null;
		String[] objectresources = { "dwc:Taxon" };
		String[] objectliterals = null;
		int limit = SearchServlet.DEFAULT_MAX_RESULT_COUNT;
		int offset = 0;
		Format format = Format.RDFXML;

		String response = SearchServlet.search(subjects, predicates, objects, objectresources, objectliterals, limit, offset, format, dao);

		Node n = new XMLReader().parse(response).getRootNode();
		assertEquals(SearchServlet.DEFAULT_MAX_RESULT_COUNT, n.getChildNodes().size());
	}

	@Test
	public void test_search_limit_and_offset() throws Exception {
		String[] subjects = null;
		String[] predicates = { "rdf:type" };
		String[] objects = null;
		String[] objectresources = { "MA.person" };
		String[] objectliterals = null;
		int limit = 10;
		int offset = 0;
		Format format = Format.RDFXML;

		String response = SearchServlet.search(subjects, predicates, objects, objectresources, objectliterals, limit, offset, format, dao);

		Node n = new XMLReader().parse(response).getRootNode();
		assertEquals(10, n.getChildNodes().size());
	}

	@Test
	public void test_search_limit_and_offset_2() throws Exception {
		String[] subjects = null;
		String[] predicates = { "rdf:type" };
		String[] objects = null;
		String[] objectresources = { "MA.person" };
		String[] objectliterals = null;
		int limit = 10;
		int offset = 0;
		Format format = Format.RDFXML;

		String response = SearchServlet.search(subjects, predicates, objects, objectresources, objectliterals, limit, offset, format, dao);

		Node n = new XMLReader().parse(response).getRootNode();
		assertEquals(10, n.getChildNodes().size());
		int i = 1;
		for (Node person : n.getChildNodes()) {
			int id = Integer.valueOf(person.getAttribute("rdf:about").replace("http://id.luomus.fi/MA.", ""));
			assertEquals(i++, id);
		}
	}

	@Test
	public void test_search_limit_and_offset_3() throws Exception {
		String[] subjects = null;
		String[] predicates = { "rdf:type" };
		String[] objects = null;
		String[] objectresources = { "MA.person" };
		String[] objectliterals = null;
		int limit = 10;
		int offset = 5;
		Format format = Format.RDFXML;

		String response = SearchServlet.search(subjects, predicates, objects, objectresources, objectliterals, limit, offset, format, dao);

		Node n = new XMLReader().parse(response).getRootNode();
		assertEquals(10, n.getChildNodes().size());
		int i = 6;
		for (Node person : n.getChildNodes()) {
			int id = Integer.valueOf(person.getAttribute("rdf:about").replace("http://id.luomus.fi/MA.", ""));
			assertEquals(i++, id);
		}
	}

	@Test
	public void test_search_no_matches() throws Exception {
		String[] subjects = null;
		String[] predicates = { "balalbaba" };
		String[] objects = null;
		String[] objectresources = { "balaba" };
		String[] objectliterals = null;
		int limit = 10;
		int offset = 5;
		Format format = Format.RDFXML;
	
		String response = SearchServlet.search(subjects, predicates, objects, objectresources, objectliterals, limit, offset, format, dao);
		assertEquals(trim("<?xmlversion='1.0'encoding='utf-8'?><rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" />"), trim(response));
	}
	
	private String trim(String string) {
		return Utils.removeWhitespace(string);
	}

}
