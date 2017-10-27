package fi.luomus.triplestore.models;

import fi.luomus.commons.containers.rdf.Qname;

public class User {

	public enum Role {ADMIN, NORMAL_USER, DESCRIPTION_WRITER};
	
	private final String personToken;
	private final Qname qname;
	private final String fullname;
	private final boolean isAdmin;
	private final Role role;
	
	public User(Qname qname, String personToken, String fullname, Role role) {
		this.qname = qname;
		this.personToken = personToken;
		this.fullname = fullname;
		this.isAdmin = role == Role.ADMIN;
		this.role = role;
	}

	public Qname getQname() {
		return qname;
	}

	public String getFullname() {
		return fullname;
	}
	
	public boolean isAdmin() {
		return isAdmin;
	}

	public String getPersonToken() {
		return personToken;
	}

	public Role getRole() {
		return role;
	}
	
}
