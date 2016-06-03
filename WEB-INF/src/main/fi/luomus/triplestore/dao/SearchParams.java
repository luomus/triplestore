package fi.luomus.triplestore.dao;

import java.util.ArrayList;
import java.util.List;

public class SearchParams {
	private final List<String> subjects = new ArrayList<>();
	private final List<String> predicates = new ArrayList<>();
	private final List<String> objects = new ArrayList<>();
	private final List<String> objectresources = new ArrayList<>();
	private final List<String> objectliterals = new ArrayList<>();
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
	public SearchParams subjects(String[] qnames) {
		addTo(subjects, qnames);
		return this;
	}
	public SearchParams predicate(String qname) {
		predicates.add(qname);
		return this;
	}
	public SearchParams predicates(String[] qnames) {
		addTo(predicates, qnames);
		return this;
	}
	public SearchParams object(String qnameOrLiteral) {
		objects.add(qnameOrLiteral);
		return this;
	}
	public SearchParams objects(String[] qnames) {
		addTo(objects, qnames);
		return this;
	}
	public SearchParams objectresource(String qname) {
		objectresources.add(qname);
		return this;
	}
	public SearchParams objectresources(String[] qnames) {
		addTo(objectresources, qnames);
		return this;
	}
	public SearchParams objectliteral(String literal) {
		objectliterals.add(literal);
		return this;
	}
	public SearchParams objectliterals(String[] literals) {
		addTo(objectliterals, literals);
		return this;
	}
	public SearchParams type(String qname) {
		this.type = qname;
		return this;
	}
	private void addTo(List<String> list, String[] qnames) {
		if (qnames == null) return;
		for (String qname : qnames) {
			list.add(qname);
		}
	}
	public List<String> getSubjects() {
		return subjects;
	}
	public List<String> getPredicates() {
		return predicates;
	}
	public List<String> getObjects() {
		return objects;
	}
	public List<String> getObjectresources() {
		return objectresources;
	}
	public List<String> getObjectliterals() {
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