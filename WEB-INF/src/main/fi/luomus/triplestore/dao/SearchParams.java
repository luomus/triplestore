package fi.luomus.triplestore.dao;

import java.util.HashSet;
import java.util.Set;

public class SearchParams {
	private final Set<String> subjects = new HashSet<>();
	private final Set<String> predicates = new HashSet<>();
	private final Set<String> objects = new HashSet<>();
	private final Set<String> objectresources = new HashSet<>();
	private final Set<String> objectliterals = new HashSet<>();
	private String type;
	private final int limit;
	private final int offset;
	public SearchParams(int limit, int offset) {
		this.limit = limit;
		this.offset = offset;
	}
	public SearchParams subject(String qname) {
		subjects.add(qname);
		return this;
	}
	public SearchParams subjects(Set<String> qnames) {
		addTo(subjects, qnames);
		return this;
	}
	public SearchParams predicate(String qname) {
		predicates.add(qname);
		return this;
	}
	public SearchParams predicates(Set<String> qnames) {
		addTo(predicates, qnames);
		return this;
	}
	public SearchParams object(String qnameOrLiteral) {
		objects.add(qnameOrLiteral);
		return this;
	}
	public SearchParams objects(Set<String> qnames) {
		addTo(objects, qnames);
		return this;
	}
	public SearchParams objectresource(String qname) {
		objectresources.add(qname);
		return this;
	}
	public SearchParams objectresources(Set<String> qnames) {
		addTo(objectresources, qnames);
		return this;
	}
	public SearchParams objectliteral(String literal) {
		objectliterals.add(literal);
		return this;
	}
	public SearchParams objectliterals(Set<String> literals) {
		addTo(objectliterals, literals);
		return this;
	}
	public SearchParams type(String qname) {
		this.type = qname;
		return this;
	}
	private void addTo(Set<String> allValues, Set<String> newValues) {
		if (newValues == null) return;
		allValues.addAll(newValues);
	}
	public Set<String> getSubjects() {
		return subjects;
	}
	public Set<String> getPredicates() {
		return predicates;
	}
	public Set<String> getObjects() {
		return objects;
	}
	public Set<String> getObjectresources() {
		return objectresources;
	}
	public Set<String> getObjectliterals() {
		return objectliterals;
	}
	public String getType() {
		return type;
	}
	public int getLimit() {
		return limit;
	}
	public int getOffset() {
		return offset;
	}
}