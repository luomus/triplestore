package fi.luomus.triplestore.models;

import fi.luomus.commons.reporting.ErrorReporter;
import fi.luomus.triplestore.dao.TriplestoreDAO;

public abstract class BaseValidator<T> {

	protected final TriplestoreDAO dao;
	protected final ErrorReporter errorReporter;
	protected final ValidationData validationData;

	public BaseValidator(TriplestoreDAO dao, ErrorReporter errorReporter) {
		this.dao = dao;
		this.errorReporter = errorReporter;
		this.validationData = new ValidationData();
	}

	protected abstract void tryValidate(T object) throws Exception;

	public ValidationData validate(T object) {
		try {
			tryValidate(object);
		} catch (Exception e) {
			setError("SYSTEM ERROR", "Could not complete validations. ICT-team has been notified. Reason: " + e.getMessage());
			errorReporter.report("Validation error", e);
		}
		return validationData;
	}

	protected void setWarning(String field, String warning) {
		validationData.setWarning(field, warning);
	}

	protected void setError(String field, String error) {
		validationData.setError(field, error);		
	}

	protected boolean given(Object o) {
		return o != null && o.toString().trim().length() > 0;
	}

}
