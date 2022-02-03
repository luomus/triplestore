package fi.luomus.triplestore.taxonomy.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.taxonomy.NoSuchTaxonException;
import fi.luomus.triplestore.taxonomy.dao.CachedLiveLoadingTaxonContainer;
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

	@Test
	public void catchingChainedNoSuchTaxonLoadException() {
		RuntimeException e = new RuntimeException("first",
				new RuntimeException("second",
						new NoSuchTaxonException(new Qname("MX.1"))));
		assertTrue(CachedLiveLoadingTaxonContainer.chainContains(NoSuchTaxonException.class, e));

		e = new RuntimeException("first",
				new RuntimeException("second",
						new RuntimeException("third",
								new NoSuchTaxonException(new Qname("MX.1")))));
		assertTrue(CachedLiveLoadingTaxonContainer.chainContains(NoSuchTaxonException.class, e));

		e = new RuntimeException("first", new RuntimeException("second", new IllegalArgumentException("third")));
		assertFalse(CachedLiveLoadingTaxonContainer.chainContains(NoSuchTaxonException.class, e));
	}

}
