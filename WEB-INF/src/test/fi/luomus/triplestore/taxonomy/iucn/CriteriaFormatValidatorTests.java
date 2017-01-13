package fi.luomus.triplestore.taxonomy.iucn;

import static org.junit.Assert.assertEquals;

import fi.luomus.triplestore.taxonomy.iucn.model.CriteriaFormatValidator;

import org.junit.Test;

public class CriteriaFormatValidatorTests {

	@Test
	public void test() {
		assertEquals(true, CriteriaFormatValidator.forCriteria("E").validate("E"));
		assertEquals(false, CriteriaFormatValidator.forCriteria("E").validate("E1"));
		
		assertEquals(true, CriteriaFormatValidator.forCriteria("A").validate("A1abc+2e"));
		
		assertEquals(true, CriteriaFormatValidator.validateJoined("A1abc+2e; E"));
	}

}
