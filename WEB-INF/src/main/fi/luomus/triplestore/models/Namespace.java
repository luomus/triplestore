package fi.luomus.triplestore.models;

import fi.luomus.commons.utils.Utils;

public class Namespace {

	private final String namespace; // EA7
	private final String personInCharge; // Jiri Kehenpää
	private final String purpose; // Digitimin digitoimat näytteet
	private final String type; // zoo
	private final String qnamePrefix; // luomus

	public Namespace(String namespace, String personInCharge, String purpose, String type, String qnamePrefix) {
		this.namespace = trim(namespace, 50);
		this.personInCharge = trim(personInCharge, 50);
		this.purpose = trim(purpose, 200);
		this.type = trim(type, 16);
		this.qnamePrefix = trim(qnamePrefix, 10);
	}

	private String trim(String s, int length) {
		if (s == null) return "";
		s = s.trim();
		return Utils.trimToLength(s, length);
	}

	public String getNamespace() {
		return namespace;
	}

	public String getPersonInCharge() {
		return personInCharge;
	}

	public String getPurpose() {
		return purpose;
	}

	public String getType() {
		return type;
	}

	public String getQnamePrefix() {
		return qnamePrefix;
	}

	@Override
	public int hashCode() {
		return namespace.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Namespace)) throw new UnsupportedOperationException("Can only compare to another " + Namespace.class.getName());
		Namespace other = (Namespace) o;
		return this.namespace.equals(other.namespace);
	}

}
