package fi.luomus.triplestore.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StringUtilsTests {

	@Test
	public void shortName() {
		assertEquals(null, SchemaClassesServlet.shortName(null));
		assertEquals("", SchemaClassesServlet.shortName(""));
		assertEquals("sortOrder", SchemaClassesServlet.shortName("sortOrder"));
		assertEquals("dataset", SchemaClassesServlet.shortName("GX.dataset"));
		assertEquals("dataset.subitem", SchemaClassesServlet.shortName("GX.dataset.subitem"));
		assertEquals("dataset.subitem:sub", SchemaClassesServlet.shortName("GX.dataset.subitem:sub"));
		assertEquals("dc:bibliographicCitation", SchemaClassesServlet.shortName("dc:bibliographicCitation"));
	}

}
