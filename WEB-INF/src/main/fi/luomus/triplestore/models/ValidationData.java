package fi.luomus.triplestore.models;

import java.util.ArrayList;
import java.util.List;

import fi.luomus.commons.json.JSONArray;
import fi.luomus.commons.json.JSONObject;
import fi.luomus.commons.utils.Utils;

public class ValidationData {

	public static class ErrorOrWarning {
		private final String field;
		private final String message;
		public ErrorOrWarning(String field, String message) {
			this.field = field;
			this.message = message;
		}
		public String getField() {
			return field;
		}
		public String getMessage() {
			return message;
		}
		@Override
		public String toString() {
			return Utils.debugS(field, message);
		}
	}
	
	private final List<ErrorOrWarning> errors = new ArrayList<ValidationData.ErrorOrWarning>();
	private final List<ErrorOrWarning> warnings = new ArrayList<ValidationData.ErrorOrWarning>();
	
	public String toJSON() {
		JSONObject json = new JSONObject();
		appendTo(json, errors, "errors");
		appendTo(json, warnings, "warnings");
		return json.toString();
	}

	private void appendTo(JSONObject json, List<ErrorOrWarning> errorsOrWarnings, String type) {
		if (!errorsOrWarnings.isEmpty()) {
			JSONArray errorArray = new JSONArray();
			json.setArray(type, errorArray);
			for (ErrorOrWarning errorOrWarning : errorsOrWarnings) {
				errorArray.appendObject(new JSONObject().setString(errorOrWarning.field, errorOrWarning.message));
			}
		}
	}

	public void setWarning(String field, String warning) {
		warnings.add(new ErrorOrWarning(field, warning));
		
	}

	public void setError(String field, String error) {
		errors.add(new ErrorOrWarning(field, error));
	}

	public List<ErrorOrWarning> getErrors() {
		return errors;
	}

	public List<ErrorOrWarning> getWarnings() {
		return warnings;
	}

	public boolean hasErrorsOrWarnings() {
		return hasErrors() || hasWarnings();
	}
	
	public boolean hasErrors() {
		return !errors.isEmpty();
	}
	
	public boolean hasWarnings() {
		return !warnings.isEmpty();
	}
}
