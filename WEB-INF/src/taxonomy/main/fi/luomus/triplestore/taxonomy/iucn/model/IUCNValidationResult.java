package fi.luomus.triplestore.taxonomy.iucn.model;

import java.util.ArrayList;
import java.util.List;

public class IUCNValidationResult {
	private final List<String> errors = new ArrayList<>();
	public boolean hasErrors() {
		return !errors.isEmpty();
	}
	public void setError(String errorMessage) {
		errors.add(errorMessage);
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
}