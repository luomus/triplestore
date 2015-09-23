package fi.luomus.triplestore.utils;

import fi.luomus.commons.containers.rdf.Context;
import fi.luomus.commons.containers.rdf.ObjectLiteral;
import fi.luomus.commons.containers.rdf.ObjectResource;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.containers.rdf.Subject;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class JenaModelToInternaModelConverter {

	private final Model jenaModel;

	public JenaModelToInternaModelConverter(Model jenaModel) {
		this.jenaModel = jenaModel;
	}

	public static class ConversionResult {
		private final Subject subject;
		private final List<Statement> statements;
		public ConversionResult(Subject subject, List<Statement> statements) {
			this.subject = subject;
			this.statements = statements;
		}
		public List<Statement> getStatements() {
			return statements;
		}
		public Subject getSubject() {
			return subject;
		}
	}

	public ConversionResult convert() {
		List<Statement> statements = new LinkedList<Statement>();
		StmtIterator iterator = jenaModel.listStatements();
		Subject subject = null;
		while (iterator.hasNext()) {
			com.hp.hpl.jena.rdf.model.Statement stmt = iterator.next();
			if (subject == null) {
				subject = new Subject(qname(stmt.getSubject()));
			} else {
				Subject otherSubject = new Subject(qname(stmt.getSubject()));
				if (!subject.getQname().equals(otherSubject.getQname())) {
					throw new IllegalStateException("Model contains triplets from more than one subject! For example: " + subject.getQname() + " and " + otherSubject.getQname());
				}
			}
			String predicateMayBeContextHacked = qname(stmt.getPredicate()).toString();
			Predicate predicate = null;
			Context context = null;
			if (predicateMayBeContextHacked.contains("_CONTEXT_")) {
				String[] parts = predicateMayBeContextHacked.split(Pattern.quote("_CONTEXT_"));
				predicate = new Predicate(parts[0]);
				context = new Context(parts[1]);
			} else {
				predicate = new Predicate(predicateMayBeContextHacked);
			}
			Statement statement = createResourceOrLiteralStatement(subject, predicate, stmt.getObject(), context);
			statements.add(statement);
		}
		return new ConversionResult(subject, statements);
	}

	private Statement createResourceOrLiteralStatement(Subject subject, Predicate predicate, RDFNode object, Context context) {
		if (object instanceof Resource) {
			return createResourceStatement(predicate, object, context);
		} else {
			return createLiteralStatement(predicate, object, context);
		}
	}

	private Statement createLiteralStatement(Predicate predicate, RDFNode object, Context context) {
		Literal literal = object.asLiteral();
		String content = literal.getString();
		String langCode = literal.getLanguage();
		ObjectLiteral objectLiteral = null;
		if (given(langCode)) {
			objectLiteral = new ObjectLiteral(content, langCode);
		} else {
			objectLiteral = new ObjectLiteral(content);
		}
		return new Statement(predicate, objectLiteral, context);
	}

	private Statement createResourceStatement(Predicate predicate, RDFNode object, Context context) {
		ObjectResource objectResource = new ObjectResource(qname(object.asResource()));
		return new Statement(predicate, objectResource, context);
	}

	private Qname qname(Resource resource) {
		return Qname.fromURI(resource.getURI());
	}

	private static boolean given(String value) {
		return value != null && value.length() > 0;
	}

	public static ConversionResult convert(String rdf) {
		Model jenaModel = JenaUtils.convert(rdf);
		return new JenaModelToInternaModelConverter(jenaModel).convert();
	}

}
