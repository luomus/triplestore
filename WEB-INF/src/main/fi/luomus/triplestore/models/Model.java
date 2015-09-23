package fi.luomus.triplestore.models;

import fi.luomus.commons.containers.rdf.ObjectLiteral;
import fi.luomus.commons.containers.rdf.ObjectResource;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.utils.InternalModelToJenaModelConverter;
import fi.luomus.triplestore.utils.JenaModelToInternaModelConverter;
import fi.luomus.triplestore.utils.JenaModelToInternaModelConverter.ConversionResult;
import fi.luomus.triplestore.utils.JenaUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Model implements Iterable<Statement> {

	private final List<Statement> statements = new ArrayList<Statement>();
	private final Map<Predicate, List<Statement>> statementMap = new HashMap<Predicate, List<Statement>>();
	private final Subject subject;

	public Model(String rdf) {
		ConversionResult result = JenaModelToInternaModelConverter.convert(rdf);
		subject = result.getSubject();
		for (Statement s : result.getStatements()) {
			addStatement(s);
		}
	}

	public Model(Qname subject) {
		this(new Subject(subject));
	}

	public Model(Subject subject) {
		this.subject = subject;
	}

	public Subject getSubject() {
		return subject;
	}

	public void addStatement(Statement statement) {
		this.statements.add(statement);
		if (!this.statementMap.containsKey(statement.getPredicate())) {
			this.statementMap.put(statement.getPredicate(), new ArrayList<Statement>());
		}
		this.statementMap.get(statement.getPredicate()).add(statement);
	}

	public List<Statement> getStatements() {
		return Collections.unmodifiableList(statements);
	}

	public String getRDF() {
		return JenaUtils.getRdf(new InternalModelToJenaModelConverter(this).getJenaModel());
	}

	@Override
	public Iterator<Statement> iterator() {
		return statements.iterator();
	}

	public void setType(String type) {
		Predicate predicate = new Predicate("rdf:type");
		removeAll(predicate);
		addStatement(new Statement(predicate, new ObjectResource(type)));
	}

	public String getType() {
		for (Statement s : this.getStatements()) {
			if (s.getPredicate().getQname().equals("rdf:type")) {
				return s.getObjectResource().getQname();
			}
		}
		return null;
	}

	public void addStatementIfObjectGiven(Predicate predicate, String objectLiteral) {
		if (given(objectLiteral)) {
			addStatement(new Statement(predicate, new ObjectLiteral(objectLiteral)));
		}
	}

	public void addStatementIfObjectGiven(String predicate, String objectLiteral) {
		this.addStatementIfObjectGiven(new Predicate(predicate), objectLiteral);
	}

	public void addStatementIfObjectGiven(Predicate predicate, String objectLiteral, String lancode) {
		if (given(objectLiteral)) {
			addStatement(new Statement(predicate, new ObjectLiteral(objectLiteral, lancode)));
		}
	}

	public void addStamentIfObjectGiven(String predicate, String objectLiteral, String lancode) {
		this.addStatementIfObjectGiven(new Predicate(predicate), objectLiteral, lancode);
	}

	public void addStatementIfObjectGiven(Predicate predicate, Qname objectResource) {
		if (given(objectResource)) {
			addStatement(new Statement(predicate, new ObjectResource(objectResource)));
		}
	}

	public void addStatementIfObjectGiven(String predicate, Qname objectResource) {
		this.addStatementIfObjectGiven(new Predicate(predicate), objectResource);
	}

	private boolean given(Object object) {
		return object != null && object.toString().trim().length() > 0;
	}

	public void removeAll(Predicate predicate) {
		Iterator<Statement> i = statements.iterator();
		while (i.hasNext()) {
			Statement statement = i.next();
			if (statement.getPredicate().equals(predicate)) {
				i.remove();
			}
		}
		statementMap.remove(predicate);
	}

	public boolean hasStatement(Statement s) {
		Predicate predicate = s.getPredicate();
		if (!statementMap.containsKey(predicate)) {
			return false;
		}
		for (Statement existing : statementMap.get(predicate)) {
			if (existing.equals(s)) {
				return true;
			}
		}
		return false;
	}

	public boolean isEmpty() {
		return statements.isEmpty();
	}

	@Override
	public String toString() {
		return Utils.debugS(subject, statements);
	}

	public boolean hasStatements(Qname predicateQname) {
		Predicate predicate = new Predicate(predicateQname);
		return statementMap.containsKey(predicate);
	}

	public boolean hasStatementsFromNonDefaultContext() {
		for (Statement s : this.getStatements()) {
			if (!s.isForDefaultContext()) return true;
		}
		return false;
	}

	public List<Statement> getStatements(String predicateQname) {
		List<Statement> statements = new ArrayList<>();
		for (Statement s : getStatements()) {
			if (s.getPredicate().getQname().equals(predicateQname)) {
				statements.add(s);
			}
		}
		return statements;
	}

}
