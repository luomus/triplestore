package fi.luomus.triplestore.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import fi.luomus.commons.http.HttpClientService;
import fi.luomus.commons.xml.Document;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ServletTests {

	private static final String TRIPLESTORE_URL = "http://127.0.0.1:8081/triplestore";
	private static HttpClientService client;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		client = new HttpClientService(TRIPLESTORE_URL, "z", "z");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
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
	public void test_no_authentication() throws Exception {
		HttpClientService noAutenticationClient = new HttpClientService();
		try {
			String response = noAutenticationClient.contentAsString(new HttpGet(TRIPLESTORE_URL + "/MA.5"));
			assertTrue(response.contains("HTTP Status 401"));
		} finally {
			if (noAutenticationClient != null) noAutenticationClient.close();
		}
	}

	@Test
	public void test_login_page_does_not_require_authentication() throws Exception {
		HttpClientService noAutenticationClient = new HttpClientService();
		try {
			String response = noAutenticationClient.contentAsString(new HttpGet(TRIPLESTORE_URL + "/login"));
			assertTrue(response.contains("<h4>Login in using your FMNH username and password</h4>"));
		} finally {
			if (noAutenticationClient != null) noAutenticationClient.close();
		}
	}
	
	@Test
	public void test_taxon_search_not_require_authentication() throws Exception {
		HttpClientService noAutenticationClient = new HttpClientService();
		try {
			String response = noAutenticationClient.contentAsString(new HttpGet(TRIPLESTORE_URL + "/taxon-search/susi"));
			assertTrue("No canis lupus in response: " + response, response.contains("scientificName=\"Canis lupus\""));
		} finally {
			if (noAutenticationClient != null) noAutenticationClient.close();
		}
	}
	
	@Test
	public void test_taxon_search_from_orphan_taxa() throws Exception {
		HttpClientService noAutenticationClient = new HttpClientService();
		try {
			String response = noAutenticationClient.contentAsString(new HttpGet(TRIPLESTORE_URL + "/taxon-search/testi?checklist=null"));
			assertTrue(response.contains("MX.210957"));
		} finally {
			if (noAutenticationClient != null) noAutenticationClient.close();
		}
	}
		
	
	// TODO suurin osa näistä on testattuna ApiServiceTest:ssä, joten ei tartte testata Servlettinä, mutta joitakin kappaleita voisi
	// esim yksi GET yksi /search yksi PUT yksi PUT single predicate
	// TODO testaa etenkin default formaatit: none -> RDF-XML/ABBREV,  json -> JSON_RDF-XML
	
	//	public void test_taxon_search_json() throws Exception {
	//		HttpGet request = new HttpGet("http://127.0.0.1:8081/triplestore-current-old/taxon-search/susi?format=json");
	//		String response = client.contentAsString(request);
	//		JSONObject o = new JSONObject(response);
	//		assertEquals("susi", o.getObject("results").getObject("exactMatch").getObject("MX.46549").getString("content"));
	//	}
	//
	//	public void test_taxon_search_jsonp() throws Exception {
	//		HttpGet request = new HttpGet("http://127.0.0.1:8081/triplestore-current-old/taxon-search/susi?format=jsonp");
	//		String response = client.contentAsString(request);
	//		assertTrue(response.contains("HTTP Status 500"));
	//		assertTrue(response.contains("Callback parameter must be given for jsonp response. Use 'callback'."));
	//	}
	//
	//	public void test_taxon_search_jsonp_2() throws Exception {
	//		HttpGet request = new HttpGet("http://127.0.0.1:8081/triplestore-current-old/taxon-search/susi?format=jsonp&callback=clbk");
	//		String response = client.contentAsString(request);
	//		assertEquals("clbk({ result: ['susi'] });", response);
	//	}
	
	
	
	//	public void test_get_abbreviated_rdf() throws Exception {
	//		HttpGet request = new HttpGet("http://127.0.0.1:8081/triplestore-current-old/MA.1");
	//		String response = client.contentAsString(request);
	//		System.out.println(response);
	//		assertTrue(response.contains("<MA.person rdf:about=\"http://tun.fi/MA.1\">"));
	//	}
	//
	//	public void test_get__format_nonabbrev_rdf() throws Exception {
	//		HttpGet request = new HttpGet("http://127.0.0.1:8081/triplestore-current-old/MA.1?format=rdfxml");
	//		String response = client.contentAsString(request);
	//		System.out.println(response);
	//		assertTrue(response.contains("<rdf:Description rdf:about=\"http://tun.fi/MA.1\">"));
	//	}
	//
	//	public void test_get__format_json() throws Exception {
	//		HttpGet request = new HttpGet("http://127.0.0.1:8081/triplestore-current-old/MA.1?format=json");
	//		String response = client.contentAsString(request);
	//		JSONObject json = new JSONObject(response);
	//		debug(json);
	//		assertEquals(1, json.getKeys().length);
	//		assertEquals("rdf:RDF", json.getKeys()[0]);
	//		assertEquals("http://tun.fi/MA.1", json.getObject("rdf:RDF").getObject("MA.person").getString("rdf:about"));
	//	}
	//
	
	//	public void test_search_limit_and_offset() throws Exception {
	//		String predicate = Utils.urlEncode("rdf:type");
	//		String object = Utils.urlEncode("MA.person");
	//		String uri = "http://127.0.0.1:8081/triplestore-current-old/search?predicate="+predicate+"&objectresource="+object+"&format=rdfxml&limit=10";
	//		HttpGet request = new HttpGet(uri);
	//		String response = client.contentAsString(request);
	//		Node n = new XMLReader().parse(response).getRootNode();
	//		assertEquals(10, n.getChildNodes().size());
	//	}
	//
	
	
	
	//
	//	public void test__delete_put_delete() throws Exception {
	//		HttpDelete delete = new HttpDelete("http://127.0.0.1:8081/triplestore-current-old/JA.123");
	//		client.contentAsString(delete);
	//
	//		HttpGet get = new HttpGet("http://127.0.0.1:8081/triplestore-current-old/JA.123");
	//		String getResponse = client.contentAsString(get);
	//		assertEquals("", getResponse);
	//
	//		String rdf = "" +
	//				"<?xml version='1.0' encoding='utf-8'?> "+
	//				"<rdf:RDF "+
	//				"    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "+
	//				"    xmlns=\"http://tun.fi/\" "+
	//				"    xmlns:dc=\"http://purl.org/dc/terms/\" "+
	//				"    xmlns:dwc=\"http://rs.tdwg.org/dwc/terms/\" "+
	//				"    xmlns:abcd=\"http://www.tdwg.org/schemas/abcd/2.06#\" "+
	//				"    xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"> "+
	//				"  <rdf:Description rdf:about=\"http://tun.fi/JA.123\"> "+
	//				"    <rdfs:label>bar</rdfs:label> "+
	//				"  </rdf:Description> "+
	//				"</rdf:RDF>";
	//		HttpPut request = new HttpPut("http://127.0.0.1:8081/triplestore-current-old/JA.123?format=RDFXMLABBREV&data="+Utils.urlEncode(rdf));
	//		String response = client.contentAsString(request);
	//
	//		Node n = new XMLReader().parse(response).getRootNode();
	//		assertEquals(1, n.getChildNodes().size());
	//		assertEquals("http://tun.fi/JA.123", n.getNode("rdf:Description").getAttribute("rdf:about"));
	//		assertEquals(1, n.getNode("rdf:Description").getChildNodes().size());
	//		assertEquals("bar", n.getNode("rdf:Description").getNode("rdfs:label").getContents());
	//
	//		client.contentAsString(delete);
	//		getResponse = client.contentAsString(get);
	//		assertEquals("", getResponse);
	//	}
	//
	//	public void test__delete__put_nonabbrev__abbrev_out() throws Exception {
	//		HttpDelete delete = new HttpDelete("http://127.0.0.1:8081/triplestore-current-old/JA.123");
	//		client.contentAsString(delete);
	//
	//		String rdf = "" +
	//				"<?xml version='1.0' encoding='utf-8'?> "+
	//				"<rdf:RDF "+
	//				"    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "+
	//				"    xmlns=\"http://tun.fi/\" "+
	//				"    xmlns:dc=\"http://purl.org/dc/terms/\" "+
	//				"    xmlns:dwc=\"http://rs.tdwg.org/dwc/terms/\" "+
	//				"    xmlns:abcd=\"http://www.tdwg.org/schemas/abcd/2.06#\" "+
	//				"    xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"> "+
	//				"  <JA.124 rdf:about=\"http://tun.fi/JA.123\"> "+
	//				"    <rdfs:label>bar</rdfs:label> "+
	//				"    <rdfs:comment xml:lang=\"fi\">baari</rdfs:comment> "+
	//				"  </JA.124> "+
	//				"</rdf:RDF>";
	//		HttpPut request = new HttpPut("http://127.0.0.1:8081/triplestore-current-old/JA.123?format=RDFXML&data="+Utils.urlEncode(rdf));
	//		String response = client.contentAsString(request);
	//
	//		System.out.println(response);
	//
	//		Node n = new XMLReader().parse(response).getRootNode();
	//		assertEquals(1, n.getChildNodes().size());
	//		assertEquals("http://tun.fi/JA.123", n.getNode("rdf:Description").getAttribute("rdf:about"));
	//		assertEquals(3, n.getNode("rdf:Description").getChildNodes().size());
	//
	//		assertEquals("http://tun.fi/JA.124", n.getNode("rdf:Description").getNode("rdf:type").getAttribute("rdf:resource"));
	//		assertEquals("bar", n.getNode("rdf:Description").getNode("rdfs:label").getContents());
	//		assertEquals("baari", n.getNode("rdf:Description").getNode("rdfs:comment").getContents());
	//		assertEquals("fi", n.getNode("rdf:Description").getNode("rdfs:comment").getAttribute("xml:lang"));
	//	}
	//
	//	public void test__put_one_predicate() throws Exception {
	//		HttpDelete delete = new HttpDelete("http://127.0.0.1:8081/triplestore-current-old/JA.123");
	//		client.contentAsString(delete);
	//
	//		HttpPut request = new HttpPut("http://127.0.0.1:8081/triplestore-current-old/JA.123?predicate_qname=rdfs:label&objectliteral=bar&langcode=sv&context_qname=");
	//		String response = client.contentAsString(request);
	//		System.out.println(response);
	//
	//		Node n = new XMLReader().parse(response).getRootNode();
	//		assertEquals(1, n.getChildNodes().size());
	//		assertEquals("http://tun.fi/JA.123", n.getNode("rdf:Description").getAttribute("rdf:about"));
	//		assertEquals(1, n.getNode("rdf:Description").getChildNodes().size());
	//
	//		assertEquals("bar", n.getNode("rdf:Description").getNode("rdfs:label").getContents());
	//		assertEquals("sv", n.getNode("rdf:Description").getNode("rdfs:label").getAttribute("xml:lang"));
	//
	//		request = new HttpPut("http://127.0.0.1:8081/triplestore-current-old/JA.123?predicate_qname=rdfs:label&objectliteral=changed&langcode=sv&context_qname=");
	//		response = client.contentAsString(request);
	//		System.out.println(response);
	//
	//		n = new XMLReader().parse(response).getRootNode();
	//		assertEquals(1, n.getChildNodes().size());
	//		assertEquals(1, n.getNode("rdf:Description").getChildNodes().size());
	//
	//		assertEquals("changed", n.getNode("rdf:Description").getNode("rdfs:label").getContents());
	//		assertEquals("sv", n.getNode("rdf:Description").getNode("rdfs:label").getAttribute("xml:lang"));
	//
	//		request = new HttpPut("http://127.0.0.1:8081/triplestore-current-old/JA.123?predicate_qname=rdfs:label&objectliteral=added&langcode=sv&context_qname=JA.1");
	//		response = client.contentAsString(request);
	//		System.out.println(response);
	//
	//		n = new XMLReader().parse(response).getRootNode();
	//		assertEquals(1, n.getChildNodes().size());
	//		assertEquals(2, n.getNode("rdf:Description").getChildNodes().size());
	//
	//		assertEquals("changed", n.getNode("rdf:Description").getNode("rdfs:label").getContents());
	//		assertEquals("sv", n.getNode("rdf:Description").getNode("rdfs:label").getAttribute("xml:lang"));
	//
	//		assertEquals("added", n.getNode("rdf:Description").getNode("rdfs:label_CONTEXT_JA.1").getContents());
	//		assertEquals("sv", n.getNode("rdf:Description").getNode("rdfs:label_CONTEXT_JA.1").getAttribute("xml:lang"));
	//	}
	//
	//	public void test__put_edit_one_predicate_removes_all_existing_subject_predicate_langcode_context_matches() throws Exception {
	//		String rdf = "" +
	//				"<?xml version='1.0' encoding='utf-8'?> "+
	//				"<rdf:RDF "+
	//				"    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "+
	//				"    xmlns=\"http://tun.fi/\" "+
	//				"    xmlns:dc=\"http://purl.org/dc/terms/\" "+
	//				"    xmlns:dwc=\"http://rs.tdwg.org/dwc/terms/\" "+
	//				"    xmlns:abcd=\"http://www.tdwg.org/schemas/abcd/2.06#\" "+
	//				"    xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"> "+
	//				"  <rdf:Description rdf:about=\"http://tun.fi/JA.123\"> "+
	//				"    <rdfs:label>bar1</rdfs:label> "+
	//				"    <rdfs:label>bar2</rdfs:label> "+
	//				"    <rdfs:label xml:lang=\"fi\">baari</rdfs:label> "+
	//				"  </rdf:Description> "+
	//				"</rdf:RDF>";
	//		HttpPut request = new HttpPut("http://127.0.0.1:8081/triplestore-current-old/JA.123?format=RDFXML&data="+Utils.urlEncode(rdf));
	//		String response = client.contentAsString(request);
	//
	//		Node n = new XMLReader().parse(response).getRootNode();
	//		assertEquals(3, n.getNode("rdf:Description").getChildNodes().size());
	//
	//		request = new HttpPut("http://127.0.0.1:8081/triplestore-current-old/JA.123?predicate_qname=rdfs:label&objectliteral=changed");
	//		response = client.contentAsString(request);
	//		System.out.println(response);
	//
	//		n = new XMLReader().parse(response).getRootNode();
	//		assertEquals(2, n.getNode("rdf:Description").getChildNodes().size());
	//		assertTrue(response.contains("<rdfs:label xml:lang=\"fi\">baari</rdfs:label>"));
	//		assertTrue(response.contains("<rdfs:label>changed</rdfs:label>"));
	//	}
	//
	//	public void test__put_context() throws Exception {
	//		String rdf = "" +
	//				"<?xml version='1.0' encoding='utf-8'?> "+
	//				"<rdf:RDF "+
	//				"    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "+
	//				"    xmlns=\"http://tun.fi/\" "+
	//				"    xmlns:dc=\"http://purl.org/dc/terms/\" "+
	//				"    xmlns:dwc=\"http://rs.tdwg.org/dwc/terms/\" "+
	//				"    xmlns:abcd=\"http://www.tdwg.org/schemas/abcd/2.06#\" "+
	//				"    xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"> "+
	//				"  <rdf:Description rdf:about=\"http://tun.fi/JA.123\"> "+
	//				"    <rdfs:label_CONTEXT_JA.1>bar</rdfs:label_CONTEXT_JA.1> "+
	//				"  </rdf:Description> "+
	//				"</rdf:RDF>";
	//		HttpPut request = new HttpPut("http://127.0.0.1:8081/triplestore-current-old/JA.123?format=RDFXMLABBREV&data="+Utils.urlEncode(rdf));
	//		String response = client.contentAsString(request);
	//		System.out.println(response);
	//
	//		Node n = new XMLReader().parse(response).getRootNode();
	//		assertEquals(1, n.getNode("rdf:Description").getChildNodes().size());
	//
	//		assertEquals("bar", n.getNode("rdf:Description").getNode("rdfs:label_CONTEXT_JA.1").getContents());
	//	}
	//
	

}
