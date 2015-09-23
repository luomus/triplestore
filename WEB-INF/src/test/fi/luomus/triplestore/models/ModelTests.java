package fi.luomus.triplestore.models;

import static org.junit.Assert.assertEquals;

import fi.luomus.commons.containers.rdf.ObjectLiteral;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;

import org.junit.Test;

public class ModelTests {

	@Test
	public void removeAllPredicates() {
		Model m = new Model(new Qname("MA.1"));
		m.addStatement(new Statement(new Predicate("MA.fullName"), new ObjectLiteral("Dare Talvitie")));
		m.addStatement(new Statement(new Predicate("MA.fullName"), new ObjectLiteral("Dare Winterroad", "en")));
		m.addStatement(new Statement(new Predicate("MA.blaa"), new ObjectLiteral("faa")));
		assertEquals(3, m.getStatements().size());
		m.removeAll(new Predicate("MA.Fullname"));
		assertEquals(3, m.getStatements().size());
		m.removeAll(new Predicate("MA.fullName"));
		assertEquals(1, m.getStatements().size());
	}

	@Test
	public void setType() {
		Model m = new Model(new Qname("MA.1"));
		assertEquals(0, m.getStatements().size());
		m.setType("MA.person");
		assertEquals(1, m.getStatements().size());
		assertEquals("rdf:type", m.getStatements().get(0).getPredicate().toString());
		assertEquals("MA.person", m.getStatements().get(0).getObjectResource().getQname());
	}
	
}
