package fi.luomus.triplestore.taxonomy.models;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import fi.luomus.triplestore.taxonomy.service.ApiChangeParentServlet;

public class MiscTests {

	@Test
	public void changeGenusName() {
		assertEquals("", ApiChangeParentServlet.changeGenus(null, null));
		assertEquals("abc", ApiChangeParentServlet.changeGenus(null, "abc"));
		assertEquals("", ApiChangeParentServlet.changeGenus("abc", null));
		assertEquals("abc", ApiChangeParentServlet.changeGenus("abc", "abc"));
		assertEquals("Gunis lupus", ApiChangeParentServlet.changeGenus("Gunis", "Canis lupus"));
		assertEquals("Gunis lupus flexis", ApiChangeParentServlet.changeGenus("Gunis", "Canis lupus flexis"));
	}
}
