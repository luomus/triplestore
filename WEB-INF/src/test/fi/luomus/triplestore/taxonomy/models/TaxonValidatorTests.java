package fi.luomus.triplestore.taxonomy.models;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.reporting.ErrorReporingToSystemErr;
import fi.luomus.commons.reporting.ErrorReporter;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.taxonomy.TaxonContainer;
import fi.luomus.triplestore.models.ValidationData;
import fi.luomus.triplestore.taxonomy.dao.ExtendedTaxonomyDAO;

public class TaxonValidatorTests {

	private static final Qname SUBGENUS = new Qname("MX.subgenus");
	private final ExtendedTaxonomyDAO dao = new TestTaxonDAO();
	private final ErrorReporter errorReporter = new ErrorReporingToSystemErr();
	private static TaxonContainer taxonContainer = null;
	
	private static final Qname SPECIES = new Qname("MX.species");

	private static class TestTaxonDAO extends TaxonomyDAOStub {
		@Override
		public List<Taxon> taxonNameExistsInChecklistForOtherTaxon(String name, Qname checklist, Qname taxonQnameToIgnore) throws Exception {
			List<Taxon> list = new ArrayList<>();
			if ("Parus major".equals(name)) {
				Taxon t =new Taxon(new Qname("MX.123"), taxonContainer);
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
		validator = new TaxonValidator(dao, errorReporter);
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
		assertEquals("[Vernacular name : Name must not contain the character '/']", result.getErrors().toString());
		assertEquals("[]", result.getWarnings().toString());
	}
	
	@Test
	public void test_vernacularName_5() {
		taxon.addVernacularName("fi", "pussihukka (harmaa värimuoto)");
		ValidationData result = validator.validate(taxon);
		assertEquals("[Vernacular name : Name must not contain the character '(']", result.getErrors().toString());
		assertEquals("[]", result.getWarnings().toString());
	}

	@Test
	public void test_vernacularName_6() {
		taxon.addVernacularName("fi", "pussihukka (harmaa värimuoto)");
		taxon.setTaxonRank(new Qname("MX.form"));
		ValidationData result = validator.validate(taxon);
		assertEquals("[]", result.getErrors().toString());
		assertEquals("[]", result.getWarnings().toString());
	}
	
	
}
