package fi.luomus.triplestore.externals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import fi.luomus.triplestore.utils.JenaUtils;

import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class FindingOutFunctionalityOfJenaModel {

	@Test
	public void whenReadingNameSpaceAndLocalNameForDefaultNamespace() {
		String rdf = "" +
				"<rdf:RDF															\n" +
				"    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"		\n" +
				"    xmlns=\"http://id.luomus.fi/\"		 							\n" +
				" >																	\n" +
				"  <rdf:Description rdf:about=\"http://id.luomus.fi/JA.123\">		\n" +
				"    <foo>Hello world!</foo>				                        \n" +
				"  </rdf:Description>												\n" +
				"</rdf:RDF>															\n";
		Model model = JenaUtils.convert(rdf);

		StmtIterator iterator = model.listStatements();
		Statement stmt = iterator.next();
		Resource subject = stmt.getSubject();


		assertEquals("http://id.luomus.fi/", subject.getNameSpace());
		assertEquals("JA.123", subject.getLocalName());
		assertEquals("http://id.luomus.fi/JA.123", subject.getURI());
		assertEquals(true, subject.isURIResource());

		assertEquals(null, model.getNsURIPrefix("http://id.luomus.fi"));
		assertEquals("rdf", model.getNsURIPrefix("http://www.w3.org/1999/02/22-rdf-syntax-ns#"));
		assertEquals(null, model.getNsURIPrefix("rdf"));
		assertEquals(null, model.getNsURIPrefix(""));
		assertEquals(null, model.getNsURIPrefix("asdasdasd"));

		assertEquals(null, model.getNsPrefixURI("http://id.luomus.fi"));
		assertEquals(null, model.getNsPrefixURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#"));
		assertEquals("http://www.w3.org/1999/02/22-rdf-syntax-ns#", model.getNsPrefixURI("rdf"));
		assertEquals("http://id.luomus.fi/", model.getNsPrefixURI(""));
		assertEquals(null, model.getNsURIPrefix("asdasdasd"));
	}

	@Test
	public void whenReadingNameSpaceAndLocalNameForDefaultNamespace_2() {
		String rdf = "" +
				"<rdf:RDF															\n" +
				"    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"		\n" +
				"    xmlns=\"http://id.luomus.fi/\"		 							\n" +
				" >																	\n" +
				"  <rdf:Description rdf:about=\"JA.123\">		\n" +
				"    <foo>Hello world!</foo>				                        \n" +
				"  </rdf:Description>												\n" +
				"</rdf:RDF>															\n";
		Model model = JenaUtils.convert(rdf);

		StmtIterator iterator = model.listStatements();
		try {
			iterator.next();
			fail("This used to fail.");
		} catch (NoSuchElementException e) {
			assertEquals("NodeToTriples iterator", e.getMessage());
		}
	}

	@Test
	public void whenReadingNameSpaceAndLocalNameForOtherNamespace() {
		String rdf = "" +
				"<rdf:RDF															\n" +
				"    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"		\n" +
				"    xmlns:abcd=\"http://www.tdwg.org/schemas/abcd/2.06#\"          \n" + 
				"    xmlns=\"http://id.luomus.fi/\"		  							\n" +
				" >																	\n" +
				"  <rdf:Description rdf:about=\"abcd:Person\">						\n" +
				"    <foo>Hello world!</foo>				                        \n" +
				"  </rdf:Description>												\n" +
				"</rdf:RDF>															\n";
		Model model = JenaUtils.convert(rdf);

		StmtIterator iterator = model.listStatements();
		Statement stmt = iterator.next();
		Resource subject = stmt.getSubject();

		assertEquals("abcd:", subject.getNameSpace());
		assertEquals("Person", subject.getLocalName());
		assertEquals("abcd:Person", subject.getURI());
		assertEquals(true, subject.isURIResource());
	}

	@Test
	public void whenReadingNameSpaceAndLocalNameForOtherNamespace_2() {
		String rdf = "" +
				"<rdf:RDF															\n" +
				"    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"		\n" +
				"    xmlns:abcd=\"http://www.tdwg.org/schemas/abcd/2.06#\"          \n" + 
				"    xmlns=\"http://id.luomus.fi/\"		  							\n" +
				" >																	\n" +
				"  <rdf:Description rdf:about=\"http://www.tdwg.org/schemas/abcd/2.06#Person\">						\n" +
				"    <foo>Hello world!</foo>				                        \n" +
				"  </rdf:Description>												\n" +
				"</rdf:RDF>															\n";
		Model model = JenaUtils.convert(rdf);

		StmtIterator iterator = model.listStatements();
		Statement stmt = iterator.next();
		Resource subject = stmt.getSubject();

		assertEquals("http://www.tdwg.org/schemas/abcd/2.06#", subject.getNameSpace());
		assertEquals("Person", subject.getLocalName());
		assertEquals("http://www.tdwg.org/schemas/abcd/2.06#Person", subject.getURI());
		assertEquals(true, subject.isURIResource());
	}

	@Test
	public void usingLiterals() {
		String rdf = "" +
				"<rdf:RDF															\n" +
				"    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"		\n" +
				"    xmlns:abcd=\"http://www.tdwg.org/schemas/abcd/2.06#\"          \n" + 
				"    xmlns=\"http://id.luomus.fi/\"		  							\n" +
				" >																	\n" +
				"  <rdf:Description rdf:about=\"http://www.tdwg.org/schemas/abcd/2.06#Person\">						\n" +
				"    <foo>Hello world!</foo>				                        \n" +
				"  </rdf:Description>												\n" +
				"</rdf:RDF>															\n";
		Model model = JenaUtils.convert(rdf);

		StmtIterator iterator = model.listStatements();
		Statement stmt = iterator.next();
		Literal literal = stmt.getLiteral();
		assertEquals("Hello world!", literal.getString());
		assertEquals("", literal.getLanguage());
	}

	@Test
	public void usingLiterals_2() {
		String rdf = "" +
				"<rdf:RDF															\n" +
				"    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"		\n" +
				"    xmlns:abcd=\"http://www.tdwg.org/schemas/abcd/2.06#\"          \n" + 
				"    xmlns=\"http://id.luomus.fi/\"		  							\n" +
				" >																	\n" +
				"  <rdf:Description rdf:about=\"http://www.tdwg.org/schemas/abcd/2.06#Person\">		\n" +
				"    <foo xml:lang=\"en\">Hello world!</foo>				                        \n" +
				"  </rdf:Description>												\n" +
				"</rdf:RDF>															\n";
		Model model = JenaUtils.convert(rdf);

		StmtIterator iterator = model.listStatements();
		Statement stmt = iterator.next();
		Literal literal = stmt.getLiteral();
		assertEquals("Hello world!", literal.getString());
		assertEquals("en", literal.getLanguage());
	}

	@Test
	public void usingLiterals_3() {
		String rdf = "" +
				"<rdf:RDF															\n" +
				"    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"		\n" +
				"    xmlns:abcd=\"http://www.tdwg.org/schemas/abcd/2.06#\"          \n" + 
				"    xmlns=\"http://id.luomus.fi/\"		  							\n" +
				" >																	\n" +
				"  <rdf:Description rdf:about=\"http://www.tdwg.org/schemas/abcd/2.06#Person\">		\n" +
				"    <foo xml:lang=\"EN\">Hello world!</foo>				                        \n" +
				"  </rdf:Description>												\n" +
				"</rdf:RDF>															\n";
		Model model = JenaUtils.convert(rdf);

		StmtIterator iterator = model.listStatements();
		Statement stmt = iterator.next();
		Literal literal = stmt.getLiteral();
		assertEquals("Hello world!", literal.getString());
		assertEquals("EN", literal.getLanguage());
	}

	@Test
	public void testingJavaSplit() {
		assertEquals("foo", "foo".split(Pattern.quote("+"))[0]);
		assertEquals(1, "foo".split(Pattern.quote("+")).length);
	}

	@Test
	public void empty_rdf_xml() {
		String rdf = "" +
				"<?xml version='1.0' encoding='utf-8'?> \n" + 
				"<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" /> ";
		Model model = JenaUtils.convert(rdf);
		assertFalse(model.listStatements().hasNext());
	}

	@Test
	public void empty_rdf_xml_2() {
		String rdf = "" +
				"<?xml version='1.0' encoding='utf-8'?> \n" + 
				"<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" ></rdf:RDF> ";
		Model model = JenaUtils.convert(rdf);
		assertFalse(model.listStatements().hasNext());
	}
	

}
