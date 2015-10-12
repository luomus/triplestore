package fi.luomus.triplestore.models;

import fi.luomus.commons.containers.rdf.Qname;

public class User {

	public enum Role {ADMIN, NORMAL_USER};
	
	private final String adUserID;
	private final Qname qname;
	private final String fullname;
	private final boolean isAdmin;
	
	public User(String adUserID, String qname, String fullname, Role role) {
		this.adUserID = adUserID;
		this.qname = new Qname(qname);
		this.fullname = fullname;
		this.isAdmin = role == Role.ADMIN;
	}
	
	public String getAdUserID() {
		return adUserID;
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
	
}
