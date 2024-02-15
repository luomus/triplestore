package fi.luomus.triplestore.taxonomy.models;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import fi.luomus.commons.containers.rdf.ObjectLiteral;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.reporting.ErrorReporter;
import fi.luomus.commons.reporting.ErrorReportingToSystemErr;
import fi.luomus.commons.taxonomy.NoSuchTaxonException;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.taxonomy.TaxonContainer;
import fi.luomus.commons.utils.Utils;
import fi.luomus.java.tests.commons.taxonomy.TaxonContainerStub;
import fi.luomus.triplestore.dao.TriplestoreDAO;
import fi.luomus.triplestore.models.ValidationData;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;

public class TaxonValidatorTests {

	private static final Qname GENUSMUS = new Qname("MX.666");
	private static final Qname SUBGENUS = new Qname("MX.subgenus");
	private final TriplestoreDAO triplestoreDAO = new TriplestoreDAOStub();
	private final ExtendedTaxonomyDAO taxonomyDAO = new TestTaxonDAO();
	private final ErrorReporter errorReporter = new ErrorReportingToSystemErr();
	private static TaxonContainer taxonContainer = new TestTaxonContainer();

	private static final Qname SPECIES = new Qname("MX.species");

	private static class TestTaxonContainer extends TaxonContainerStub {
		private static final Qname GENUS = new Qname("MX.genus");
		@Override
		public boolean hasTaxon(Qname q) {
			if (q.equals(GENUSMUS)) return true;
			return false;
		}
		@Override
		public Taxon getTaxon(Qname q) throws NoSuchTaxonException {
			if (q.equals(GENUSMUS)) {
				Taxon t = new Taxon(GENUSMUS, this);
				t.setScientificName("Genusmus");
				t.setTaxonRank(GENUS);
				return t;
			}
			return null;
		}
	}

	private static class TestTaxonDAO extends TaxonomyDAOStub {
		@Override
		public List<Taxon> taxonNameExistsInChecklistForOtherTaxon(String name, Taxon taxon) throws Exception {
			List<Taxon> list = new ArrayList<>();
			if ("Parus major".equals(name)) {
				Taxon t = new Taxon(new Qname("MX.123"), taxonContainer);
				t.setScientificName("Parus major");
				t.setTaxonRank(SPECIES);
				list.add(t);
			}
			return list;
		}
	}

	private TaxonValidator validator;
	private Taxon taxon;

	@Before
	public void before() {
		validator = new TaxonValidator(triplestoreDAO, taxonomyDAO, errorReporter);
		taxon = new Taxon(new Qname("MX.1"), taxonContainer);
	}

	@Test
	public void test() {
		ValidationData result = validator.validate(taxon);
		assertEquals(0, result.getErrors().size());
		assertEquals(0, result.getWarnings().size());
	}

	@Test
	public void test_used_sciname() {
		taxon.setScientificName("Parus major");
		taxon.setTaxonRank(SPECIES);
		ValidationData result = validator.validate(taxon);
		assertEquals(1, result.getErrors().size());
		assertEquals(0, result.getWarnings().size());

		assertEquals("[Scientific name : Name already used in this checklist for taxon: MX.123, Parus major [species]]", result.getErrors().toString());
	}

	@Test
	public void test_sciname_and_rank() {
		taxon.setScientificName("Canis lupus");
		taxon.setTaxonRank(SPECIES);
		ValidationData result = validator.validate(taxon);
		assertEquals("[]", result.getErrors().toString());
		assertEquals("[]", result.getWarnings().toString());
	}

	@Test
	public void test_sciname_and_rank_2() {
		taxon.setScientificName("Canis (Canis)");
		taxon.setTaxonRank(SUBGENUS);
		ValidationData result = validator.validate(taxon);
		assertEquals("[]", result.getErrors().toString());
		assertEquals("[]", result.getWarnings().toString());
	}

	@Test
	public void test_sciname_and_rank_3() {
		taxon.setScientificName("Canis (Canis)");
		taxon.setTaxonRank(new Qname("MX.genus"));
		ValidationData result = validator.validate(taxon);
		assertEquals("[Scientific name : Must not contain the character ' ']", result.getErrors().toString());
		assertEquals("[]", result.getWarnings().toString());
	}

	@Test
	public void test_sciname_and_rank_4() {
		taxon.setScientificName("Canis");
		taxon.setTaxonRank(new Qname("MX.genus"));
		ValidationData result = validator.validate(taxon);
		assertEquals("[]", result.getErrors().toString());
		assertEquals("[]", result.getWarnings().toString());
	}

	@Test
	public void test_sciname_and_rank_5() {
		taxon.setScientificName("Canis");
		taxon.setTaxonRank(SPECIES);
		ValidationData result = validator.validate(taxon);
		assertEquals("[Scientific name : Must contain a space for species and subspecies (etc). Use full form: [Genus] [specific epithet]]", result.getErrors().toString());
		assertEquals("[]", result.getWarnings().toString());
	}

	@Test
	public void test_sciname_6() {
		taxon.setScientificName("\"Phylpylyrus\"");
		taxon.setTaxonRank(new Qname("MX.class"));
		ValidationData result = validator.validate(taxon);
		assertEquals("[]", result.getErrors().toString());
		assertEquals("[]", result.getWarnings().toString());
	}

	@Test
	public void test_sciname_7() {
		taxon.setScientificName("Phylpylyrus \"maximus\"");
		taxon.setTaxonRank(new Qname("MX.species"));
		ValidationData result = validator.validate(taxon);
		assertEquals("[Scientific name : Must not contain the character '\"']", result.getErrors().toString());
		assertEquals("[]", result.getWarnings().toString());
	}

	@Test
	public void test_sciname_8() {
		taxon.setScientificName("Unrankedilius taxonomius");
		ValidationData result = validator.validate(taxon);
		assertEquals("[]", result.getErrors().toString());
		assertEquals("[]", result.getWarnings().toString());
	}

	@Test
	public void test_sciname_vs_genusname() {
		taxon.setScientificName("Genusmus perus");
		taxon.setTaxonRank(SPECIES);
		taxon.setParentQname(GENUSMUS); // Name = Genusmus
		ValidationData result = validator.validate(taxon);
		assertEquals("[]", result.getErrors().toString());
		assertEquals("[]", result.getWarnings().toString());
	}

	@Test
	public void test_sciname_vs_genusname_2() {
		taxon.setScientificName("Xxxxx perus");
		taxon.setTaxonRank(SPECIES);
		taxon.setParentQname(GENUSMUS); // Name = Genusmus
		ValidationData result = validator.validate(taxon);
		assertEquals("[Scientific name : Genus of a species must match the name of the parent genus (Genusmus)]", result.getErrors().toString());
		assertEquals("[]", result.getWarnings().toString());
	}

	@Test
	public void test_sciname_vs_genusname_3() {
		taxon.setScientificName("Genusmus (Genusmus) perus");
		taxon.setTaxonRank(SPECIES);
		taxon.setParentQname(GENUSMUS); // Name = Genusmus
		ValidationData result = validator.validate(taxon);
		assertEquals("[]", result.getErrors().toString());
		assertEquals("[]", result.getWarnings().toString());
	}

	@Test
	public void test_sciname_vs_genusname_4() {
		taxon.setScientificName("Xxxx (Genusmus) perus");
		taxon.setTaxonRank(SPECIES);
		taxon.setParentQname(GENUSMUS); // Name = Genusmus
		ValidationData result = validator.validate(taxon);
		assertEquals("[Scientific name : Genus of a species must match the name of the parent genus (Genusmus)]", result.getErrors().toString());
		assertEquals("[]", result.getWarnings().toString());
	}

	@Test
	public void speciesAggregate() {
		taxon.setScientificName("Larus fuscus/argentatus/marinus");
		taxon.setTaxonRank(new Qname("MX.speciesAggregate"));
		ValidationData result = validator.validate(taxon);
		assertEquals("[]", result.getErrors().toString());
		assertEquals("[]", result.getWarnings().toString());
	}

	@Test
	public void speciesAggregate_2() {
		taxon.setScientificName("Larus aggr.");
		taxon.setTaxonRank(new Qname("MX.speciesAggregate"));
		ValidationData result = validator.validate(taxon);
		assertEquals("[Scientific name : Species aggregate name must contain the character '/']", result.getErrors().toString());
		assertEquals("[]", result.getWarnings().toString());
	}

	@Test
	public void test_vernacularName() {
		taxon.addVernacularName("fi", "pussihukka");
		ValidationData result = validator.validate(taxon);
		assertEquals("[]", result.getErrors().toString());
		assertEquals("[]", result.getWarnings().toString());
	}

	@Test
	public void test_vernacularName_2() {
		taxon.addVernacularName("fi", "-hukka");
		ValidationData result = validator.validate(taxon);
		assertEquals("[Vernacular name : Name should begin with an alpha. For example write the complete name \"jättiputket -ryhmä\", not just \"-ryhmä\".]", result.getErrors().toString());
		assertEquals("[]", result.getWarnings().toString());
	}

	@Test
	public void test_vernacularName_3() {
		taxon.addVernacularName("fi", "pussi-iiris");
		ValidationData result = validator.validate(taxon);
		assertEquals("[]", result.getErrors().toString());
		assertEquals("[]", result.getWarnings().toString());
	}

	@Test
	public void test_vernacularName_4() {
		taxon.addVernacularName("fi", "pussihukka/pussisusi");
		ValidationData result = validator.validate(taxon);
		assertEquals("[Vernacular name : Name should not contain the character '/']", result.getWarnings().toString());
		assertEquals("[]", result.getErrors().toString());
	}

	@Test
	public void test_vernacularName_5() {
		taxon.addVernacularName("fi", "pussihukka (harmaa värimuoto)");
		ValidationData result = validator.validate(taxon);
		assertEquals("[Vernacular name : Name should not contain the character '(']", result.getWarnings().toString());
		assertEquals("[]", result.getErrors().toString());
	}

	@Test
	public void test_vernacularName_6() {
		taxon.addVernacularName("fi", "pussihukka (harmaa värimuoto)");
		taxon.setTaxonRank(new Qname("MX.form"));
		ValidationData result = validator.validate(taxon);
		assertEquals("[]", result.getErrors().toString());
		assertEquals("[]", result.getWarnings().toString());
	}

	@Test
	public void test_vernacularName_7() {
		taxon.addVernacularName("fi", "viiksi'vallu");
		ValidationData result = validator.validate(taxon);
		assertEquals("[Vernacular name : Name should not contain the character ''']", result.getWarnings().toString());
		assertEquals("[]", result.getErrors().toString());
	}

	@Test
	public void test_vernacularName_8() {
		taxon.addVernacularName("en", "Viiksi'vallu");
		ValidationData result = validator.validate(taxon);
		assertEquals("[]", result.getErrors().toString());
		assertEquals("[]", result.getWarnings().toString());
	}

	@Test
	public void test_description_fields_1() {
		List<Statement> s = Utils.list(new Statement(new Predicate("MX.descriptionText"), new ObjectLiteral("<p>foobar</p>", "fi")));
		ValidationData result = validator.validateDescriptions(s);
		assertEquals("[]", result.getErrors().toString());
		assertEquals("[]", result.getWarnings().toString());
	}

	@Test
	public void test_description_fields_1_2() {
		List<Statement> s = Utils.list(new Statement(new Predicate("MX.descriptionText"), new ObjectLiteral("<a href=\"http://..\">foobar</a>", "fi")));
		ValidationData result = validator.validateDescriptions(s);
		assertEquals("[]", result.getErrors().toString());
		assertEquals("[]", result.getWarnings().toString());
	}

	@Test
	public void test_description_fields_2() {
		List<Statement> s = Utils.list(new Statement(new Predicate("MX.descriptionText"), new ObjectLiteral("foobar <iframe>", "fi")));
		ValidationData result = validator.validateDescriptions(s);
		assertEquals("[Yleiskuvaus - General description : Unallowed tag: iframe. They were removed from the saved content! Allowed tags are: p, a, b, strong, i, em, ul, li]", result.getErrors().toString());
		assertEquals("[]", result.getWarnings().toString());
	}

	@Test
	public void test_description_fields_2_1() {
		List<Statement> s = Utils.list(new Statement(new Predicate("MX.descriptionText"), new ObjectLiteral("foobar < iframe src=\"\">", "fi")));
		ValidationData result = validator.validateDescriptions(s);
		assertEquals("[Yleiskuvaus - General description : Unallowed tag: iframe. They were removed from the saved content! Allowed tags are: p, a, b, strong, i, em, ul, li]", result.getErrors().toString());
		assertEquals("[]", result.getWarnings().toString());
	}

	@Test
	public void test_description_fields_3() {
		String longtext = "Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text. Very long text.";
		assertEquals(4879, Utils.countOfUTF8Bytes(longtext));
		List<Statement> s = Utils.list(new Statement(new Predicate("MX.descriptionText"), new ObjectLiteral(longtext, "fi")));
		ValidationData result = validator.validateDescriptions(s);
		assertEquals("[Yleiskuvaus - General description : The text was too long and it has been shortened!]", result.getErrors().toString());
		assertEquals("[]", result.getWarnings().toString());
	}

	@Test
	public void test_description_fields_4() {
		List<Statement> s = Utils.list(new Statement(new Predicate("MX.descriptionText"), new ObjectLiteral("<p style=\"color: red;\">", "fi")));
		ValidationData result = validator.validateDescriptions(s);
		assertEquals("[]", result.getErrors().toString());
		assertEquals("[Yleiskuvaus - General description : Custom styles are discouraged]", result.getWarnings().toString());
	}

}
