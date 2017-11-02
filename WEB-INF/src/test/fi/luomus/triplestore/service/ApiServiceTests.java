package fi.luomus.triplestore.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fi.luomus.commons.config.Config;
import fi.luomus.commons.config.ConfigReader;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.utils.Utils;
import fi.luomus.commons.xml.Document.Node;
import fi.luomus.commons.xml.XMLReader;
import fi.luomus.triplestore.dao.DataSourceDefinition;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.dao.TriplestoreDAO.ResultType;
import fi.luomus.triplestore.dao.TriplestoreDAOConst;
import fi.luomus.triplestore.dao.TriplestoreDAOImple;
import fi.luomus.triplestore.service.EditorBaseServlet.Format;
import fi.luomus.triplestore.utils.StringUtils;

public class ApiServiceTests {

	private static final String RDF_DESCRIPTION = "rdf:Description";
	private static final String MX_ORIGIN_AND_DISTRIBUTION_TEXT = "MX.originAndDistributionText";
	private static TriplestoreDAO dao;
	private static DataSource dataSource;
	private static final Qname TEST_RESOURCE_QNAME = new Qname("JA.123");
	private static final Subject TEST_RESOURCE = new Subject(TEST_RESOURCE_QNAME);


	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Config config = new ConfigReader("C:/apache-tomcat/app-conf/triplestore-v2.properties");
		TriplestoreDAOConst.SCHEMA = config.get("LuontoDbName");
		dataSource = DataSourceDefinition.initDataSource(config.connectionDescription());
		dao = new TriplestoreDAOImple(dataSource, TriplestoreDAO.TEST_USER);
	}

	@AfterClass
	public static void afterClass() throws Exception {
		dao.delete(TEST_RESOURCE);
		dataSource.close();
	}

	@Test
	public void test_get_by_qname_notgiven() throws Exception {
		String response = ApiServlet.get(new Qname(null), ResultType.NORMAL, Format.RDFXML, dao);
		assertNull(response);
	}

	@Test
	public void test_get_by_qname_nonexisting() throws Exception {
		String response = ApiServlet.get(new Qname("nonexistig"), ResultType.NORMAL, Format.RDFXML, dao);
		assertNull(response);
	}

	@Test
	public void test_get_by_qname_existing_but_no_statements() throws Exception {
		String response = ApiServlet.get(new Qname("rdfs:range"), ResultType.NORMAL, Format.RDFXML, dao);
		assertNull(response);
	}

	private String trim(String string) {
		return Utils.removeWhitespace(string);
	}

	@Test
	public void test_get_by_qname_rdfxml() throws Exception {
		String response = ApiServlet.get(new Qname("MA.1"), ResultType.NORMAL, Format.RDFXML, dao);
		assertTrue(response.contains("<rdf:Description rdf:about=\"http://tun.fi/MA.1\">"));

	}

	@Test
	public void test_get_by_qname_rdfxml_abbrev() throws Exception {
		String response = ApiServlet.get(new Qname("MA.1"), ResultType.NORMAL, Format.RDFXMLABBREV, dao);
		assertTrue(response.contains("<MA.person rdf:about=\"http://tun.fi/MA.1\">"));

	}

	@Test
	public void test_get_by_qname_json() throws Exception {
		String response1 = ApiServlet.get(new Qname("MA.1"), ResultType.NORMAL, Format.JSON, dao);
		String response2 = ApiServlet.get(new Qname("MA.1"), ResultType.NORMAL, Format.JSON_RDFXMLABBREV, dao);
		assertEquals(response1, response2);
		assertTrue(trim(response1).contains(trim("\"MA.person\": { ")));
		assertTrue(trim(response1).contains(trim("\"rdf:about\": \"http://tun.fi/MA.1\",")));

	}

	@Test
	public void test_get_by_qname_json_non_abbrev() throws Exception {
		String response = ApiServlet.get(new Qname("MA.1"), ResultType.NORMAL, Format.JSON_RDFXML, dao);
		assertTrue(trim(response).contains(trim("\"rdf:Description\": { ")));
		assertTrue(trim(response).contains(trim("\"rdf:about\": \"http://tun.fi/MA.1\",")));
		assertTrue(trim(response).contains(trim("\"rdf:type\": { \"rdf:resource\": \"http://tun.fi/MA.person\" },")));		
	}

	@Test
	public void test_get_by_qname_resulttype_chain() throws Exception {
		String response = ApiServlet.get(new Qname("MX.37602"), ResultType.CHAIN, Format.RDFXMLABBREV, dao);
		Node n = new XMLReader().parse(response).getRootNode();

		assertEquals(1, n.getChildNodes("MX.taxon").size());
		Node animalia = n.getNode("MX.taxon");
		assertEquals("Animalia", animalia.getNode("MX.scientificName").getContents());

		assertEquals(1, animalia.getChildNodes("MX.isPartOf").size());
		Node eucarya = animalia.getNode("MX.isPartOf").getNode("MX.taxon");
		assertEquals("Eucarya", eucarya.getNode("MX.scientificName").getContents());

		assertEquals(1, eucarya.getChildNodes("MX.isPartOf").size());
		Node biota = eucarya.getNode("MX.isPartOf").getNode("MX.taxon");
		assertEquals("Biota", biota.getNode("MX.scientificName").getContents());

		assertEquals(0, biota.getChildNodes("MX.isPartOf").size());
	}

	@Test
	public void test_get_by_qname_resulttype_children() throws Exception {
		String response = ApiServlet.get(new Qname("MX.7"), ResultType.CHILDREN, Format.RDFXML, dao);

		Node n = new XMLReader().parse(response).getRootNode();
		Set<String> taxonRanks = new HashSet<String>();
		for (Node child : n.getChildNodes()) {
			taxonRanks.add(child.getNode("MX.taxonRank").getAttribute("rdf:resource"));
		}
		assertTrue(taxonRanks.contains("http://tun.fi/MX.species"));
		assertTrue(taxonRanks.contains("http://tun.fi/MX.genus"));
		assertTrue(taxonRanks.contains("http://tun.fi/MX.family"));
	}


	@Test
	public void test_resulttype_tree() throws Exception {
		String response = ApiServlet.get(new Qname("luomus:EA4.0WB"), ResultType.TREE, Format.RDFXMLABBREV, dao);

		Node n = new XMLReader().parse(response).getRootNode();
		assertEquals(1, n.getChildNodes().size());
		n = n.getNode("MY.document");
		assertTrue(n.getChildNodes().size() > 10);
		assertEquals(1, n.getChildNodes("MZ.hasPart").size());
		assertEquals(1, n.getNode("MZ.hasPart").getChildNodes().size());
		Node gathering = n.getNode("MZ.hasPart").getNode("MY.gathering");
		assertTrue(gathering.getChildNodes().size() > 5);
		assertEquals(1, gathering.getChildNodes("MZ.hasPart").size());
		assertEquals(1, gathering.getNode("MZ.hasPart").getChildNodes().size());
		Node unit = gathering.getNode("MZ.hasPart").getNode("MY.unit");
		assertEquals("http://tun.fi/MY.sexF", unit.getNode("MY.sex").getAttribute("rdf:resource"));
		assertEquals(1, unit.getChildNodes("MZ.hasPart").size());
		assertEquals(1, unit.getNode("MZ.hasPart").getChildNodes().size());
		Node identification = unit.getNode("MZ.hasPart").getNode("MY.identification");
		assertEquals("http://tun.fi/MY.210289", identification.getAttribute("rdf:about"));
		assertEquals("Apamea crenata", identification.getNode("MY.taxon").getContents());
	}

	@Test
	public void test__put_and_delete() throws Exception {
		dao.delete(TEST_RESOURCE);

		String response = ApiServlet.get(TEST_RESOURCE_QNAME, ResultType.NORMAL, Format.RDFXML, dao);
		assertNull(response);

		String data = "" +
				"<rdf:RDF "+
				"    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "+
				"    xmlns=\"http://tun.fi/\" "+
				"    xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"> "+
				"  <rdf:Description rdf:about=\"http://tun.fi/JA.123\"> "+
				"    <rdfs:label>bar</rdfs:label> "+
				"  </rdf:Description> "+
				"</rdf:RDF>";
		ApiServlet.put(TEST_RESOURCE_QNAME, data, Format.RDFXML, dao);

		response = ApiServlet.get(TEST_RESOURCE_QNAME, ResultType.NORMAL, Format.RDFXML, dao);
		assertEquals(Utils.cleanForCompare(data), Utils.cleanForCompare(response));

		response = ApiServlet.delete(TEST_RESOURCE_QNAME, Format.RDFXML, dao);
		Node root = new XMLReader().parse(response).getRootNode();
		assertFalse(root.hasChildNodes());
	}

	@Test
	public void test__put__data_is_abbrev_but_format_is_nonabbrev() throws Exception {
		dao.delete(TEST_RESOURCE);

		String data = "" +
				"<rdf:RDF "+
				"    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "+
				"    xmlns=\"http://tun.fi/\" "+
				"    xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"> "+
				"  <JA.124 rdf:about=\"http://tun.fi/JA.123\"> "+
				"    <rdfs:label>bar</rdfs:label> "+
				"    <rdfs:comment xml:lang=\"fi\">baari</rdfs:comment> "+
				"  </JA.124> "+
				"</rdf:RDF>";
		ApiServlet.put(TEST_RESOURCE_QNAME, data, Format.RDFXML, dao); // put succeeds even when format is incorrect

		String response = ApiServlet.get(TEST_RESOURCE_QNAME, ResultType.NORMAL, Format.RDFXML, dao); // response is in the format given as parameter
		assertTrue(response.contains("<rdf:Description rdf:about=\"http://tun.fi/JA.123\">"));
		assertTrue(response.contains("<rdf:type rdf:resource=\"http://tun.fi/JA.124\""));
	}

	@Test
	public void test__put_one_predicate() throws Exception {
		dao.delete(TEST_RESOURCE);

		// Put    rdfs:label "bar" @ sv for null context
		String predicateQname = "rdfs:label";
		String objectResource = null;
		String objectLiteral = "bar";
		String langCode = "sv";
		String contextQname = null;

		ApiServlet.put(TEST_RESOURCE_QNAME, predicateQname, objectResource, objectLiteral, langCode, contextQname, dao);
		String response = ApiServlet.get(TEST_RESOURCE_QNAME, ResultType.NORMAL, Format.RDFXML, dao);

		Node n = new XMLReader().parse(response).getRootNode();
		assertEquals(1, n.getChildNodes().size());
		assertEquals("http://tun.fi/JA.123", n.getNode(RDF_DESCRIPTION).getAttribute("rdf:about"));
		assertEquals(1, n.getNode(RDF_DESCRIPTION).getChildNodes().size());

		assertEquals("bar", n.getNode(RDF_DESCRIPTION).getNode("rdfs:label").getContents());
		assertEquals("sv", n.getNode(RDF_DESCRIPTION).getNode("rdfs:label").getAttribute("xml:lang"));

		// Put    rdfs:label "changed" @ sv for null context
		predicateQname = "rdfs:label";
		objectResource = null;
		objectLiteral = "changed";
		langCode = "sv";
		contextQname = null;

		ApiServlet.put(TEST_RESOURCE_QNAME, predicateQname, objectResource, objectLiteral, langCode, contextQname, dao);
		response = ApiServlet.get(TEST_RESOURCE_QNAME, ResultType.NORMAL, Format.RDFXML, dao);

		n = new XMLReader().parse(response).getRootNode();
		assertEquals(1, n.getChildNodes().size());
		assertEquals(1, n.getNode(RDF_DESCRIPTION).getChildNodes().size());

		assertEquals("changed", n.getNode(RDF_DESCRIPTION).getNode("rdfs:label").getContents());
		assertEquals("sv", n.getNode(RDF_DESCRIPTION).getNode("rdfs:label").getAttribute("xml:lang"));

		// Put  rdfs:label "added" @ sv for context JA.1
		predicateQname = "rdfs:label";
		objectResource = null;
		objectLiteral = "added";
		langCode = "sv";
		contextQname = "JA.1";

		ApiServlet.put(TEST_RESOURCE_QNAME, predicateQname, objectResource, objectLiteral, langCode, contextQname, dao);
		response = ApiServlet.get(TEST_RESOURCE_QNAME, ResultType.NORMAL, Format.RDFXML, dao);

		n = new XMLReader().parse(response).getRootNode();
		assertEquals(1, n.getChildNodes().size());
		assertEquals(2, n.getNode(RDF_DESCRIPTION).getChildNodes().size());

		assertEquals("changed", n.getNode(RDF_DESCRIPTION).getNode("rdfs:label").getContents());
		assertEquals("sv", n.getNode(RDF_DESCRIPTION).getNode("rdfs:label").getAttribute("xml:lang"));

		assertEquals("added", n.getNode(RDF_DESCRIPTION).getNode("rdfs:label_CONTEXT_JA.1").getContents());
		assertEquals("sv", n.getNode(RDF_DESCRIPTION).getNode("rdfs:label_CONTEXT_JA.1").getAttribute("xml:lang"));
	}

	@Test
	public void test__put_edit_one_predicate_removes_all_existing_subject_predicate_langcode_context_matches() throws Exception {
		String rdf = "" +
				"<?xml version='1.0' encoding='utf-8'?> "+
				"<rdf:RDF "+
				"    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "+
				"    xmlns=\"http://tun.fi/\" "+
				"    xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"> "+
				"  <rdf:Description rdf:about=\"http://tun.fi/JA.123\"> "+
				"    <rdfs:label>bar1</rdfs:label> "+
				"    <rdfs:label>bar2</rdfs:label> "+
				"    <rdfs:label xml:lang=\"fi\">baari</rdfs:label> "+
				"  </rdf:Description> "+
				"</rdf:RDF>";
		ApiServlet.put(TEST_RESOURCE_QNAME, rdf, Format.RDFXML, dao);
		String response = ApiServlet.get(TEST_RESOURCE_QNAME, ResultType.NORMAL, Format.RDFXML, dao);

		Node n = new XMLReader().parse(response).getRootNode();
		assertEquals(3, n.getNode(RDF_DESCRIPTION).getChildNodes().size()); // Before changing rdfs:label for no language and null context there are 3 statements

		// Put    rdfs:label "changed" @ no language for null context
		String predicateQname = "rdfs:label";
		String objectResource = null;
		String objectLiteral = "changed";
		String langCode = null;
		String contextQname = null;

		ApiServlet.put(TEST_RESOURCE_QNAME, predicateQname, objectResource, objectLiteral, langCode, contextQname, dao);
		response = ApiServlet.get(TEST_RESOURCE_QNAME, ResultType.NORMAL, Format.RDFXML, dao);

		n = new XMLReader().parse(response).getRootNode();
		assertTrue(response.contains("<rdfs:label xml:lang=\"fi\">baari</rdfs:label>"));
		assertTrue(response.contains("<rdfs:label>changed</rdfs:label>"));
		assertEquals(2, n.getNode(RDF_DESCRIPTION).getChildNodes().size()); // Statement for language "fi" should remain!
	}

	@Test
	public void test__put_one__predicate_with_objectresource() throws Exception {
		String rdf = "" +
				"<?xml version='1.0' encoding='utf-8'?> "+
				"<rdf:RDF "+
				"    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "+
				"    xmlns=\"http://tun.fi/\" "+
				"    xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"> "+
				"  <rdf:Description rdf:about=\"http://tun.fi/JA.123\"> "+
				"    <MZ.isPartOf rdf:resource=\"http://tun.fi/JA.1\" /> "+
				"  </rdf:Description> "+
				"</rdf:RDF>";
		ApiServlet.put(TEST_RESOURCE_QNAME, rdf, Format.RDFXML, dao);
		String response = ApiServlet.get(TEST_RESOURCE_QNAME, ResultType.NORMAL, Format.RDFXML, dao);

		Node n = new XMLReader().parse(response).getRootNode();
		assertEquals(1, n.getNode(RDF_DESCRIPTION).getChildNodes().size());

		// Put    MZ.isPartOf "JA.2" for null context
		String predicateQname = "MZ.isPartOf";
		String objectResource = "JA.2";
		String objectLiteral = null;
		String langCode = null;
		String contextQname = null;

		ApiServlet.put(TEST_RESOURCE_QNAME, predicateQname, objectResource, objectLiteral, langCode, contextQname, dao);
		response = ApiServlet.get(TEST_RESOURCE_QNAME, ResultType.NORMAL, Format.RDFXML, dao);

		n = new XMLReader().parse(response).getRootNode();
		assertTrue(response.contains("<MZ.isPartOf rdf:resource=\"http://tun.fi/JA.2\""));
		assertEquals(1, n.getNode(RDF_DESCRIPTION).getChildNodes().size());
	}

	@Test
	public void test__put_one__predicate_with_objectresource_to_different_context() throws Exception {
		String rdf = "" +
				"<?xml version='1.0' encoding='utf-8'?> "+
				"<rdf:RDF "+
				"    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "+
				"    xmlns=\"http://tun.fi/\" "+
				"    xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"> "+
				"  <rdf:Description rdf:about=\"http://tun.fi/JA.123\"> "+
				"    <MZ.isPartOf rdf:resource=\"http://tun.fi/JA.1\"/>"+
				"  </rdf:Description> "+
				"</rdf:RDF>";
		ApiServlet.put(TEST_RESOURCE_QNAME, rdf, Format.RDFXML, dao);
		String response = ApiServlet.get(TEST_RESOURCE_QNAME, ResultType.NORMAL, Format.RDFXML, dao);

		Node n = new XMLReader().parse(response).getRootNode();
		assertEquals(1, n.getNode(RDF_DESCRIPTION).getChildNodes().size());

		// Put    MZ.isPartOf "JA.2" for null context
		String predicateQname = "MZ.isPartOf";
		String objectResource = "JA.2";
		String objectLiteral = null;
		String langCode = null;
		String contextQname = "JA.2";

		ApiServlet.put(TEST_RESOURCE_QNAME, predicateQname, objectResource, objectLiteral, langCode, contextQname, dao);
		response = ApiServlet.get(TEST_RESOURCE_QNAME, ResultType.NORMAL, Format.RDFXML, dao);
		System.out.println(response);

		n = new XMLReader().parse(response).getRootNode();
		assertTrue(response.contains("<MZ.isPartOf rdf:resource=\"http://tun.fi/JA.1\""));
		assertTrue(response.contains("<MZ.isPartOf_CONTEXT_JA.2 rdf:resource=\"http://tun.fi/JA.2\""));
		assertEquals(2, n.getNode(RDF_DESCRIPTION).getChildNodes().size());
	}

	@Test
	public void test__use_put_one_predicate_to_delete() throws Exception {
		String rdf = "" +
				"<?xml version='1.0' encoding='utf-8'?> "+
				"<rdf:RDF "+
				"    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "+
				"    xmlns=\"http://tun.fi/\" "+
				"    xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"> "+
				"  <rdf:Description rdf:about=\"http://tun.fi/JA.123\"> "+
				"    <rdfs:label xml:lang=\"fi\">foo</rdfs:label> "+
				"    <rdfs:label xml:lang=\"sv\">foopåsvenska</rdfs:label> "+
				"    <rdfs:label_CONTEXT_JA.1 xml:lang=\"fi\">foodifferentcontext</rdfs:label_CONTEXT_JA.1> "+
				"  </rdf:Description> "+
				"</rdf:RDF>";
		ApiServlet.put(TEST_RESOURCE_QNAME, rdf, Format.RDFXML, dao);
		String response = ApiServlet.get(TEST_RESOURCE_QNAME, ResultType.NORMAL, Format.RDFXML, dao);
		System.out.println(response);
		Node n = new XMLReader().parse(response).getRootNode();
		assertEquals(3, n.getNode(RDF_DESCRIPTION).getChildNodes().size());

		// Put    rdfs:label "" for language "fi" and null context
		String predicateQname = "rdfs:label";
		String objectResource = null;
		String objectLiteral = "";
		String langCode = "fi";
		String contextQname = null;

		ApiServlet.put(TEST_RESOURCE_QNAME, predicateQname, objectResource, objectLiteral, langCode, contextQname, dao);
		response = ApiServlet.get(TEST_RESOURCE_QNAME, ResultType.NORMAL, Format.RDFXML, dao);
		System.out.println(response);

		n = new XMLReader().parse(response).getRootNode();
		assertEquals(2, n.getNode(RDF_DESCRIPTION).getChildNodes().size());
	}

	@Test
	public void test__get_several_qnames() throws Exception {
		List<Qname> qnames = Utils.list(new Qname("MX.1"), new Qname("MX.2"), new Qname("MX.3"));
		String response = ApiServlet.get(qnames, ResultType.NORMAL, Format.RDFXML, dao);

		Node n = new XMLReader().parse(response).getRootNode();
		assertEquals(3, n.getChildNodes().size());

		int i = 1;
		for (Node child : n.getChildNodes()) {
			String id = child.getAttribute("rdf:about");
			assertEquals("http://tun.fi/MX." + i, id);
			i++;
		}
	}

	@Test
	public void tooLongLiteralContent() throws Exception {
		String literal = "<p>Perunarutto on peräisin Perun Andeilta tai Meksikosta, josta se levisi 1840-luvun alussa Pohjois-Amerikkaan. Euroopassa taudista on ensimmäisiä havaintoja Belgiasta 1843 tai 1844. Vuoden 1845 syksyllä tauti levisi kulovalkean tavoin Euroopan rannikkoseuduilla ja erityisesti Irlannissa, jossa se tuhosi valtaosan saaren perunasadosta. Tuhot toistuivat seuraavina kesinä entistä pahempina ja seurauksena arviolta miljoona irlantilaista nääntyi nälkään ja arviolta saman verran asukkaita joutui lähtemään siirtolaisiksi Amerikkaan ja Australiaan.</p><p>Suomessa perunaruton esiintymisestä Etelä-Suomessa raportoitiin ensimmäisen kerran eri sanomalehdissä syksyllä 1847. Seuraavana vuonna perunaruttoa esiintyi lehtitietojen mukaan hyvin tuhoisana etenkin Turun ja Viipurin seuduilla, mutta tautia esiintyi myös Pohjois-Suomessa aina Tornionjokilaaksoa myöten. 1800-luvun puolivälistä aina 1900-luvulle perunaruttoa esiintyi sanomalehtitietojen mukaan perunassa miltei joka kesä. Maanlaajuisia perunaruttoepidemioita oli kuitenkin enintään muutamana vuotena vuosikymmentä kohden. Tauti ilmaantui perunapelloille yleensä elokuun puolivälin jälkeen ja satotappiot johtuivat pääsääntöisesti mukuloiden pilaantumisesta kellareissa.</p><p>1900-luvulle asti Suomen maaseudulla asuva valtaväestö ei ollut erityisen riippuvainen perunasta, sitä kasvatettiin ensisijaisesti karjan rehuksi. Täten perunarutto ei Suomessa koskaan aiheuttanut maanlaajuista nälänhätää. Ruttovuosista joutuivat eniten kärsimään köyhät tilattomat ja nopeasti kehittyvien teollisuuspaikkakuntien työväki, joiden tärkeä energianlähde oli palstaviljelmillä kasvatettu peruna.</p><p>1900-luvulla julkaistiin tutkimusraportteja perunaruton esiintymisestä eri vuosikymmenillä ja niiden perusteella taudin esiintyminen oli varsin samanlaista aina 1990-luvulle asti &ndash; ensimmäiset ruttohavainnot tehtiin yleensä elokuun jälkipuoliskolla ja vakavia epidemioita esiintyi 2&ndash;3 kertaa vuosikymmenessä. Yksittäisillä viljelmillä tuhot toki saattoivat olla hyvin suuria. 1950-luvulta alkaen kemiallisia rutontorjunta-aineita alettiin aktiivisesti markkinoida viljelijöille, mutta kemiallinen rutontorjunta alkoi yleistyä vasta 1970-luvulla. Ruton hallitsemiseksi tuolloin tarvittiin enintään 1&ndash;3 torjuntakäsittelyä elo-syyskuussa.</p><p>1980-luvun lopulla perunarutosta tuli aivan uudenlainen ongelmatauti. Meksikosta levisi ensin Manner-Eurooppaan ja sittemmin 1990-luvun kuluessa miltei kaikille muillekin maapallon perunantuotantoalueille hyvin aggressiivinen uusi ruttopopulaatio. Vanha maailmanlaajuinen perunaruton populaatio koostui vain taudinaiheuttajan toisesta pariutumistyypistä (&rdquo;sukupuoli&rdquo;) A1. Toinen pariutumistyyppi A2 oli yleinen ainoastaan Meksikossa. Uusi erittäin nopeasti levittäytynyt populaatio sisälsi molemmat pariutumistyypit. Seurauksena uusi perunarutto alkoi tuottaa maassa säilyviä suvullisia munaitiöitä. Munaitiöiden avulla perunaruton aiheuttaja pystyy säilymään talven yli ilman eläviä perunan mukuloita. Nykyisin myös hyvin aggressiiviset ruttokannat, jotka aiemmin hävisivät tuhotessaan nopeasti perunan mukulat talven aikana, kykenevät säilymään munaitiöinä maassa.</p><p>Vakavin seuraus uuden ruttopopulaation leviämisestä Suomeen 1990-luvulla oli ruttoepidemioiden alun aikaistuminen. Maassa talvehtineet taudinaiheuttajan munaitiöt voivat tartuttaa perunan ja aiheuttaa vakavan epidemian jo taimettumisvaiheessa. Mukuloissa talvehtinut taudinaiheuttaja tarvitsee useita viikkoja lisääntyäkseen tasolle, joka saa aikaan vakavan ruttoepidemian. Suomessa ruton ilmaantuminen perunapelloille aikaistui1980-luvulta 2000-luvun alkuun mennessä 4&ndash;5 viikolla: kun rutto aiemmin iski elokuun puolivälissä, uusi rutto ilmaantuikin pelloille heti juhannuksen jälkeen.</p><p>Aluksi uusi rutto aiheutti pahoja ongelmia, koska torjuntatoimiin ryhdyttiin liian myöhään.&nbsp; Nyt uuden ruton kanssa on opittu elämään ja ajoittamaan torjuntatoimet oikein. Luomutuotannolle ja kotitarveviljelijöille aikaisin alkava perunarutto on kuitenkin pahoina ruttovuosina hyvin suuri ongelma.</p><p>&nbsp;</p>";
		assertEquals(4111, literal.length());
		assertEquals(4221, StringUtils.countOfUTF8Bytes(literal));
		ApiServlet.put(TEST_RESOURCE_QNAME, MX_ORIGIN_AND_DISTRIBUTION_TEXT, null, literal, "fi", null, dao);
		String response = ApiServlet.get(TEST_RESOURCE_QNAME, ResultType.NORMAL, Format.RDFXML, dao);
		Node n = new XMLReader().parse(response).getRootNode();
		literal = n.getNode(RDF_DESCRIPTION).getNode(MX_ORIGIN_AND_DISTRIBUTION_TEXT).getContents();
		assertEquals(3859, literal.length());
		assertEquals(3977, StringUtils.countOfUTF8Bytes(literal));
	}

	@Test
	public void sanitizeLiterals_1() throws Exception {
		String givenLiteral = "<p>Foobar</p>";
		ApiServlet.put(TEST_RESOURCE_QNAME, MX_ORIGIN_AND_DISTRIBUTION_TEXT, null, givenLiteral, "fi", null, dao);
		String response = ApiServlet.get(TEST_RESOURCE_QNAME, ResultType.NORMAL, Format.RDFXML, dao);
		Node n = new XMLReader().parse(response).getRootNode();
		String storedLiteral = n.getNode(RDF_DESCRIPTION).getNode(MX_ORIGIN_AND_DISTRIBUTION_TEXT).getContents();
		assertEquals(givenLiteral, storedLiteral);
	}

	@Test
	public void sanitizeLiterals_2() throws Exception {
		String givenLiteral = "Foo <a href=\"http://...\">bar</a>";
		ApiServlet.put(TEST_RESOURCE_QNAME, MX_ORIGIN_AND_DISTRIBUTION_TEXT, null, givenLiteral, "fi", null, dao);
		String response = ApiServlet.get(TEST_RESOURCE_QNAME, ResultType.NORMAL, Format.RDFXML, dao);
		Node n = new XMLReader().parse(response).getRootNode();
		String storedLiteral = n.getNode(RDF_DESCRIPTION).getNode(MX_ORIGIN_AND_DISTRIBUTION_TEXT).getContents();
		assertEquals(givenLiteral, storedLiteral);
	}

	@Test
	public void sanitizeLiterals_3() throws Exception {
		String givenLiteral = "Foo <iframe src=\"http://...\"></iframe>";
		ApiServlet.put(TEST_RESOURCE_QNAME, MX_ORIGIN_AND_DISTRIBUTION_TEXT, null, givenLiteral, "fi", null, dao);
		String response = ApiServlet.get(TEST_RESOURCE_QNAME, ResultType.NORMAL, Format.RDFXML, dao);
		Node n = new XMLReader().parse(response).getRootNode();
		String storedLiteral = n.getNode(RDF_DESCRIPTION).getNode(MX_ORIGIN_AND_DISTRIBUTION_TEXT).getContents();
		String expected = "Foo";
		assertEquals(expected, storedLiteral);
	}
	
	@Test
	public void sanitizeLiterals_4() throws Exception {
		String givenLiteral = "Foo <iframe src=\"http://...\"></a>";
		ApiServlet.put(TEST_RESOURCE_QNAME, MX_ORIGIN_AND_DISTRIBUTION_TEXT, null, givenLiteral, "fi", null, dao);
		String response = ApiServlet.get(TEST_RESOURCE_QNAME, ResultType.NORMAL, Format.RDFXML, dao);
		Node n = new XMLReader().parse(response).getRootNode();
		String storedLiteral = n.getNode(RDF_DESCRIPTION).getNode(MX_ORIGIN_AND_DISTRIBUTION_TEXT).getContents();
		String expected = "Foo </a>";
		assertEquals(expected, storedLiteral);
	}
	
	@Test
	public void sanitizeLiterals_5() throws Exception {
		String givenLiteral = "Foo <p>bar";
		ApiServlet.put(TEST_RESOURCE_QNAME, MX_ORIGIN_AND_DISTRIBUTION_TEXT, null, givenLiteral, "fi", null, dao);
		String response = ApiServlet.get(TEST_RESOURCE_QNAME, ResultType.NORMAL, Format.RDFXML, dao);
		Node n = new XMLReader().parse(response).getRootNode();
		String storedLiteral = n.getNode(RDF_DESCRIPTION).getNode(MX_ORIGIN_AND_DISTRIBUTION_TEXT).getContents();
		String expected = "Foo <p>bar</p>";
		assertEquals(expected, storedLiteral);
	}
	
	@Test
	public void sanitizeLiterals_6() throws Exception {
		String givenLiteral = "Foo <p>bar</a> ";
		ApiServlet.put(TEST_RESOURCE_QNAME, MX_ORIGIN_AND_DISTRIBUTION_TEXT, null, givenLiteral, "fi", null, dao);
		String response = ApiServlet.get(TEST_RESOURCE_QNAME, ResultType.NORMAL, Format.RDFXML, dao);
		Node n = new XMLReader().parse(response).getRootNode();
		String storedLiteral = n.getNode(RDF_DESCRIPTION).getNode(MX_ORIGIN_AND_DISTRIBUTION_TEXT).getContents();
		String expected = "Foo <p>bar</p>";
		assertEquals(expected, storedLiteral);
	}
	
	@Test
	public void whitespace_nonbreaking() throws Exception {
		String nonBreakingChar = String.valueOf(Character.toChars(Character.codePointAt("\u00A0", 0))); 
		String givenLiteral = nonBreakingChar + " Foo " + nonBreakingChar + " " + nonBreakingChar;
		
		ApiServlet.put(TEST_RESOURCE_QNAME, MX_ORIGIN_AND_DISTRIBUTION_TEXT, null, givenLiteral, "fi", null, dao);
		
		String response = ApiServlet.get(TEST_RESOURCE_QNAME, ResultType.NORMAL, Format.RDFXML, dao);
		Node n = new XMLReader().parse(response).getRootNode();
		String storedLiteral = n.getNode(RDF_DESCRIPTION).getNode(MX_ORIGIN_AND_DISTRIBUTION_TEXT).getContents();
		String expected = "&#xa0; Foo &#xa0; &#xa0;";
		assertEquals(expected, storedLiteral);
	}
	
}
