package fi.luomus.triplestore.utils;

import static org.junit.Assert.assertEquals;

import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.triplestore.models.Model;
import fi.luomus.triplestore.utils.JenaModelToInternaModelConverter.ConversionResult;

import java.util.List;

import org.junit.Test;

public class JenaModelToInternalModelsTests {

	@Test
	public void transforminSimpleModel() {
		String rdf = "" +
				"<rdf:RDF															\n" +
				"    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"		\n" +
				"    xmlns=\"http://id.luomus.fi/\"							    > 	\n" + 
				"  <rdf:Description rdf:about=\"http://id.luomus.fi/JA.123\">		\n" +
				"    <foo xml:lang=\"en\">Hello world!</foo>				        \n" +
				"  </rdf:Description>												\n" +
				"</rdf:RDF>													\n";
		Model model = new Model(rdf);
		List<Statement> statements = model.getStatements();

		assertEquals(1, statements.size());
		Statement statement = statements.get(0);
		assertEquals("JA.123", model.getSubject().getQname());
		assertEquals("foo", statement.getPredicate().getQname());
		assertEquals("Hello world!", statement.getObjectLiteral().getContent());
		assertEquals("en", statement.getObjectLiteral().getLangcode());
		assertEquals(null, statement.getContext());		
		assertEquals(true, statement.isLiteralStatement());
		assertEquals(false, statement.isResourceStatement());
		assertEquals(true, statement.isForDefaultContext());
	}

	@Test
	public void transforminSimpleModel_using_qname_as_resource_identifier_which_is_a_nono() {
		String rdf = "" +
				"<rdf:RDF															\n" +
				"    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"		\n" +
				"    xmlns=\"http://id.luomus.fi/\"							    > 	\n" + 
				"  <rdf:Description rdf:about=\"JA.123\">		\n" +
				"    <foo xml:lang=\"en\">Hello world!</foo>				        \n" +
				"  </rdf:Description>												\n" +
				"</rdf:RDF>															\n";
		ConversionResult result = JenaModelToInternaModelConverter.convert(rdf);
		assertEquals(0, result.getStatements().size());
		assertEquals(null, result.getSubject());
	}

	@Test
	public void transforminSimpleModel_with_subject_using_qname_of_external_namespace__and_couple_special_cases() {
		String rdf = "" +
				"<rdf:RDF															\n" +
				"    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"		\n" +
				"    xmlns=\"http://id.luomus.fi/\"							    > 	\n" + 
				"  <rdf:Description rdf:about=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#Something\">					\n" +
				"    <foo xml:lang=\"EN\"></foo>				        			\n" +
				"  </rdf:Description>												\n" +
				"</rdf:RDF>															\n";
		ConversionResult result = JenaModelToInternaModelConverter.convert(rdf);
		List<Statement> statements = result.getStatements();

		assertEquals(1, statements.size());
		Statement statement = statements.get(0);
		assertEquals("rdf:Something", result.getSubject().getQname());
		assertEquals("foo", statement.getPredicate().getQname());
		assertEquals("", statement.getObjectLiteral().getContent());
		assertEquals("en", statement.getObjectLiteral().getLangcode());
		assertEquals(true, statement.isLiteralStatement());
		assertEquals(false, statement.isResourceStatement());
	}

	@Test
	public void transforminSimpleModel_with_subject_using_uri_of_external_namespace() {
		String rdf = "" +
				"<rdf:RDF															\n" +
				"    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"		\n" +
				"    xmlns=\"http://id.luomus.fi/\"							    > 	\n" + 
				"  <rdf:Description rdf:about=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#Something\">					\n" +
				"    <foo xml:lang=\"EN\"></foo>				        			\n" +
				"  </rdf:Description>												\n" +
				"</rdf:RDF>															\n";
		ConversionResult result = JenaModelToInternaModelConverter.convert(rdf);
		assertEquals("rdf:Something", result.getSubject().getQname());
	}

	@Test
	public void transforminSimpleModel_with_context() {
		String rdf = "" +
				"<rdf:RDF															\n" +
				"    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"		\n" +
				"    xmlns=\"http://id.luomus.fi/\"							    > 	\n" + 
				"  <rdf:Description rdf:about=\"http://id.luomus.fi/JA.123\">		\n" +
				"    <foo_CONTEXT_LA.1 xml:lang=\"en\">Hello world!</foo_CONTEXT_LA.1>	 \n" +
				"  </rdf:Description>												\n" +
				"</rdf:RDF>													\n";
		Model model = new Model(rdf);
		List<Statement> statements = model.getStatements();

		assertEquals(1, statements.size());
		Statement statement = statements.get(0);
		assertEquals("JA.123", model.getSubject().getQname());
		assertEquals("foo", statement.getPredicate().getQname());
		assertEquals("Hello world!", statement.getObjectLiteral().getContent());
		assertEquals("en", statement.getObjectLiteral().getLangcode());
		assertEquals("LA.1", statement.getContext().getQname());		
		assertEquals(true, statement.isLiteralStatement());
		assertEquals(false, statement.isResourceStatement());
		assertEquals(false, statement.isForDefaultContext());
	}

	@Test
	public void transformUsingCustomDefinedNamespace() {
		String rdf = "" +
				" <rdf:RDF " +
				"    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"  " +
				"    xmlns:j.1=\"http://tun.fi/\"  " +
				"    xmlns:j.0=\"http://id.luomus.fi/\">  " +
				"  <j.0:MM.image rdf:about=\"http://id.luomus.fi/MM.86040\">  " +
				"    <j.0:MM.largeURL>my uri</j.0:MM.largeURL>  " +
				"    <j.0:MM.something rdf:resource=\"http://id.luomus.fi/MM.1\" />" +
				"    <j.1:FOO rdf:resource=\"http://tun.fi/BAR\" />" +
				"  </j.0:MM.image> " + 
				"</rdf:RDF>";

		Model model = new Model(rdf);
		assertEquals("MM.86040", model.getSubject().getQname());
		List<Statement> statements = model.getStatements();
		assertEquals(4, statements.size());
		
		for (Statement s : statements) {
			if (s.getPredicate().toString().equals("rdf:type")) {
				assertEquals("MM.image", s.getObjectResource().getQname());
			} else if (s.getPredicate().toString().equals("MM.largeURL")) {
				assertEquals("my uri", s.getObjectLiteral().getContent());
			} else if (s.getPredicate().toString().equals("MM.something")) {
				assertEquals("MM.1", s.getObjectResource().getQname());
			} else if (s.getPredicate().toString().equals("tun:FOO")) {
				assertEquals("tun:BAR", s.getObjectResource().getQname());
			}
		}
	}

}

