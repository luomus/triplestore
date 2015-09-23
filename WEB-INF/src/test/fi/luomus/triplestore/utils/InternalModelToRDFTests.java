package fi.luomus.triplestore.utils;

import fi.luomus.commons.containers.rdf.Context;
import fi.luomus.commons.containers.rdf.ObjectLiteral;
import fi.luomus.commons.containers.rdf.ObjectResource;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.triplestore.models.Model;
import fi.luomus.triplestore.service.ApiServiceTests;

import org.junit.Assert;
import org.junit.Test;

public class InternalModelToRDFTests {

	private void assertEquals(String expected, String actual) {
		Assert.assertEquals(ApiServiceTests.cleanForCompare(expected), ApiServiceTests.cleanForCompare(actual));
	}

	@Test
	public void generateSimpleRDF() {
		Model model = new Model(new Qname("JA.123"));
		model.addStatement(new Statement(new Predicate("foo"), new ObjectLiteral("bar")));

		String expected = "" +
				"<rdf:RDF															\n" +
				"    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"		\n" +
				"    xmlns=\"http://id.luomus.fi/\"							    > 	\n" + 
				"  <rdf:Description rdf:about=\"http://id.luomus.fi/JA.123\">		\n" +
				"    <foo>bar</foo>				        							\n" +
				"  </rdf:Description>												\n" +
				"</rdf:RDF>															\n";
		assertEquals(expected, model.getRDF());
	}

	@Test
	public void generateSimpleRDF_with_lancode() {
		Model model = new Model(new Qname("JA.123"));
		model.addStatement(new Statement(new Predicate("foo"), new ObjectLiteral("bar", "en")));

		String expected = "" +
				"<rdf:RDF															\n" +
				"    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"		\n" +
				"    xmlns=\"http://id.luomus.fi/\"							    > 	\n" + 
				"  <rdf:Description rdf:about=\"http://id.luomus.fi/JA.123\">		\n" +
				"    <foo xml:lang=\"en\">bar</foo>				 					\n" +
				"  </rdf:Description>												\n" +
				"</rdf:RDF>															\n";
		assertEquals(expected, model.getRDF());
	}

	@Test
	public void generateSimpleRDF_with_external_resource_as_predicate() {
		Model model = new Model(new Qname("JA.123"));
		model.addStatement(new Statement(new Predicate("abcd:Person"), new ObjectResource("MA.1")));

		String expected = "" +
				"<rdf:RDF															\n" +
				"    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"		\n" +
				"    xmlns=\"http://id.luomus.fi/\"							  		\n" +
				"    xmlns:abcd=\"http://www.tdwg.org/schemas/abcd/2.06#\" 		 > 	\n" + 
				"  <rdf:Description rdf:about=\"http://id.luomus.fi/JA.123\">		\n" +
				"    <abcd:Person rdf:resource=\"http://id.luomus.fi/MA.1\" />		\n" +
				"  </rdf:Description>												\n" +
				"</rdf:RDF>															\n";
		assertEquals(expected, model.getRDF());
	}

	@Test
	public void generateSimpleRDF_with_default_namespace_resource_as_object() {
		Model model = new Model(new Qname("JA.123"));
		model.addStatement(new Statement(new Predicate("foo"), new ObjectResource("MA.1")));

		String expected = "" +
				"<rdf:RDF															\n" +
				"    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"		\n" +
				"    xmlns=\"http://id.luomus.fi/\"							  	> 	\n" + 
				"  <rdf:Description rdf:about=\"http://id.luomus.fi/JA.123\">		\n" +
				"    <foo rdf:resource=\"http://id.luomus.fi/MA.1\" />				\n" +
				"  </rdf:Description>												\n" +
				"</rdf:RDF>															\n";
		assertEquals(expected, model.getRDF());
	}

	@Test
	public void generateSimpleRDF_from_external_namespace() {
		Model model = new Model(new Qname("rdf:Something"));
		model.addStatement(new Statement(new Predicate("rdf:Is"), new ObjectResource("rdf:Pretty")));

		String expected = "" +
				"<rdf:RDF																				\n" +
				"    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"	> 						\n" + 
				"  <rdf:Description rdf:about=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#Something\">	\n" +
				"    <rdf:Is rdf:resource=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#Pretty\" />		\n" +
				"  </rdf:Description>																	\n" +
				"</rdf:RDF>																				\n";
		assertEquals(expected, model.getRDF());
	}

	@Test
	public void generateSimpleRDF_with_context() {
		Model model = new Model(new Qname("JA.123"));

		model.addStatement(new Statement(new Predicate("foo"), new ObjectLiteral("bar"), new Context("LA.1")));

		String expected = "" +
				"<rdf:RDF															\n" +
				"    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"		\n" +
				"    xmlns=\"http://id.luomus.fi/\"							    > 	\n" + 
				"  <rdf:Description rdf:about=\"http://id.luomus.fi/JA.123\">		\n" +
				"    <foo_CONTEXT_LA.1>bar</foo_CONTEXT_LA.1>				        \n" +
				"  </rdf:Description>												\n" +
				"</rdf:RDF>															\n";
		assertEquals(expected, model.getRDF());
	}

}
