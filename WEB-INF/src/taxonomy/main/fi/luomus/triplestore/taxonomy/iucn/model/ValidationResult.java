package fi.luomus.triplestore.taxonomy.iucn.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ValidationResult {
	private final List<String> errors = new ArrayList<>();
	private final Set<String> erroreousFields = new HashSet<>();
	public boolean hasErrors() {
		return !errors.isEmpty();
	}
	public void setError(String errorMessage, String fieldQname) {
		errors.add(errorMessage);
		if (fieldQname != null) {
			erroreousFields.add(fieldQname);
		}
	}
	public String getErrors() {
		StringBuilder b = new StringBuilder();
		b.append("<ul>");
		for (String error : errors) {
			b.append("<li>").append(error).append("</li>");
		}
		b.append("</ul>");
		return b.toString();
	}
	
	public List<String> listErrors() {
		return errors;
	}
	public Set<String> getErroreousFields() {
		return erroreousFields;
	}
	public void addErrorField(String fieldQname) {
		erroreousFields.add(fieldQname);
	}
}