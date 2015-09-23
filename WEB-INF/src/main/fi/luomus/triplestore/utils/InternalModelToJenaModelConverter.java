package fi.luomus.triplestore.utils;

import fi.luomus.commons.containers.rdf.ObjectLiteral;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.RdfResource;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.containers.rdf.Subject;
import fi.luomus.commons.utils.Utils;
import fi.luomus.triplestore.models.Model;

import java.util.Collection;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class InternalModelToJenaModelConverter {

	private final Collection<Model> models;
	private final com.hp.hpl.jena.rdf.model.Model jenaModel = JenaUtils.newDefaultModel();

	public InternalModelToJenaModelConverter(Model model) {
		this.models = Utils.list(model);
	}

	public InternalModelToJenaModelConverter(Collection<Model> models) {
		this.models = models;
	}

	public com.hp.hpl.jena.rdf.model.Model getJenaModel() {
		for (Model model : models) {
			addToJenaModel(model);
		}
		return jenaModel;
	}

	private void addToJenaModel(Model model) {
		addToNamespace(model.getSubject());
		for (Statement statement : model) {
			addNamespaces(statement);
			addToJenaModel(model.getSubject(), statement);
		}
	}

	private void addNamespaces(Statement statement) {
		addToNamespace(statement.getPredicate());
		if (statement.isResourceStatement()) {
			addToNamespace(statement.getObjectResource());
		}
	}

	private void addToNamespace(RdfResource resource) {
		jenaModel.setNsPrefix(resource.getNamespacePrefix(), resource.getNamespaceURI());
	}

	private void addToJenaModel(Subject subjectOfModel, Statement statement) {
		Resource subject = jenaModel.createProperty(subjectOfModel.getURI());
		Property predicate = jenaModel.createProperty(contectHackedPredicate(statement).getURI());
		if (statement.isLiteralStatement()) {
			ObjectLiteral literal = statement.getObjectLiteral();
			if (literal.hasLangcode()) {
				jenaModel.add(subject, predicate, jenaModel.createLiteral(literal.getContent(), literal.getLangcode()));
			} else {
				jenaModel.add(subject, predicate, jenaModel.createLiteral(literal.getContent()));
			}
		} else {
			jenaModel.add(subject, predicate, jenaModel.createProperty(statement.getObjectResource().getURI()));
		}
	}

	private Predicate contectHackedPredicate(Statement statement) {
		Predicate p = statement.isForDefaultContext() ? statement.getPredicate() : new Predicate(statement.getPredicate().getQname() + "_CONTEXT_" + statement.getContext().getQname());
		return p;
	}

}
