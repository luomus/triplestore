package fi.luomus.triplestore.dao;

public class MissingResourceException extends Exception {

	private static final long serialVersionUID = 8524017468208613088L;
	private final String resourceQname;

	public MissingResourceException(String message, String resourceQname) {
		super(message);
		this.resourceQname = resourceQname;
	}

	public String getResourceQname() {
		return resourceQname;
	}

}
