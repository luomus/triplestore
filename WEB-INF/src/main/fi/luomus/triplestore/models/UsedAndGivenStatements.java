package fi.luomus.triplestore.models;

import fi.luomus.commons.containers.SingleValueObject;
import fi.luomus.commons.containers.rdf.Context;
import fi.luomus.commons.containers.rdf.Predicate;
import fi.luomus.commons.containers.rdf.Statement;
import fi.luomus.commons.utils.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UsedAndGivenStatements {

	private final List<Statement> givenStatements = new ArrayList<Statement>();
	private final Set<Used> used = new HashSet<Used>();

	public static class Used extends SingleValueObject {
		private final String stringRepresentation;
		private final Predicate predicate;
		private final Context context;
		private final String langcode;
		public Used(Statement statement) {
			this(statement.getPredicate(), statement.getContext(), statement.isLiteralStatement() ? statement.getObjectLiteral().getLangcode() : null);
		}
		public Used(Predicate predicate, Context context, String langcode) {
			if (!given(langcode)) langcode = null;
			this.predicate = predicate;
			this.context = context;
			this.langcode = langcode;
			this.stringRepresentation = Utils.debugS(predicate, context, langcode);
		}
		@Override
		protected String getValue() {
			return stringRepresentation;
		}
		public Predicate getPredicate() {
			return predicate;
		}
		public Context getContext() {
			return context;
		}
		public String getLangcode() {
			return langcode;
		}
		private boolean given(String s) {
			return s != null && s.trim().length() > 0;
		}
	}

	public UsedAndGivenStatements addStatement(Statement statement) {
		used.add(new Used(statement));
		givenStatements.add(statement);
		return this;
	}

	public Collection<Used> getUsed() {
		return Collections.unmodifiableCollection(used);
	}

	public Collection<Statement> getGivenStatements() {
		return givenStatements;
	}

	public UsedAndGivenStatements addUsed(Predicate predicate, Context context, String langCode) {
		used.add(new Used(predicate, context, langCode));
		return this;
	}

}