package fi.luomus.triplestore.models;

import java.util.Set;

import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.utils.Utils;

public class User {

	public enum Role {ADMIN, NORMAL_USER, DESCRIPTION_WRITER}

	private static final Set<Qname> IUCN_ADMINS = Utils.set(new Qname("MA.842"), new Qname("MA.1397"), new Qname("MA.15"), new Qname("MA.1363"), new Qname("MA.1283"));

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

	public boolean isIucnAdmin() {
		return isAdmin() || IUCN_ADMINS.contains(qname);
	}

}
