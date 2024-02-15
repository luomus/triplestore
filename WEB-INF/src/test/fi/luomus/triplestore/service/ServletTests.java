package fi.luomus.triplestore.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fi.luomus.commons.http.HttpClientService;
import fi.luomus.commons.xml.Document;

public class ServletTests {

	private static final String TRIPLESTORE_URL = "http://127.0.0.1:8081/triplestore";
	private static HttpClientService client;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		client = new HttpClientService(TRIPLESTORE_URL, "z", "z");
	}

	@AfterClass
	public static void tearDownAfterClass() {
		if (client != null) {
			client.close();
		}
	}

	@After
	public void tearDown() throws Exception {
		if (client != null) {
			HttpDelete delete = new HttpDelete(TRIPLESTORE_URL + "/JA.123");
			client.execute(delete).close();
		}
	}

	@Test
	public void test_authentication() throws Exception {
		Document response = client.contentAsDocument(new HttpGet(TRIPLESTORE_URL + "/MA.5"));
		assertEquals("http://tun.fi/MA.5", response.getRootNode().getNode("MA.person").getAttribute("rdf:about"));
	}

	@Test
	public void test_no_authentication() {
		HttpClientService noAutenticationClient = new HttpClientService();
		try {
			try {
				noAutenticationClient.contentAsString(new HttpGet(TRIPLESTORE_URL + "/MA.5"));
			} catch (Exception e) {
				assertTrue(e.getMessage().startsWith("Request returned 401"));
			}
		} finally {
			noAutenticationClient.close();
		}
	}

	@Test
	public void test_login_page_does_not_require_authentication() throws Exception {
		HttpClientService noAutenticationClient = new HttpClientService();
		try {
			String response = noAutenticationClient.contentAsString(new HttpGet(TRIPLESTORE_URL + "/login"));
			assertTrue(response.contains("Log in to Triplestore Editor"));
		} finally {
			noAutenticationClient.close();
		}
	}

	@Test
	public void test_taxon_search_not_require_authentication() throws Exception {
		HttpClientService noAutenticationClient = new HttpClientService();
		try {
			String response = noAutenticationClient.contentAsString(new HttpGet(TRIPLESTORE_URL + "/taxon-search/susi"));
			assertTrue("No canis lupus in response: " + response, response.contains("scientificName=\"Canis lupus\""));
		} finally {
			noAutenticationClient.close();
		}
	}

	@Test
	public void test_taxon_search_from_orphan_taxa() throws Exception {
		HttpClientService noAutenticationClient = new HttpClientService();
		try {
			String response = noAutenticationClient.contentAsString(new HttpGet(TRIPLESTORE_URL + "/taxon-search/testi?checklist=null"));
			assertTrue(response.contains("MX.210957"));
		} finally {
			noAutenticationClient.close();
		}
	}

}
