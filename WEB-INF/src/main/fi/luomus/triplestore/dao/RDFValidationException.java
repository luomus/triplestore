package fi.luomus.triplestore.dao;

import fi.luomus.commons.containers.rdf.Model;

public class RDFValidationException extends RuntimeException {

	private static final long serialVersionUID = -1547484377160485757L;

	public RDFValidationException(Model model, Exception e) {
		super("Invalid data: " + debug(model), e);
	}

	private static String debug(Model model) {
		try {
			return model.toString();
		} catch (Exception e) {
			return "Debug of data failed as well!";
		}
	}

}
