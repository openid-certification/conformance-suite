package net.openid.conformance.vci10issuer.util;

import java.io.Serial;

public class JsonSchemaValidationException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	private JsonSchemaValidationResult validationResult;

	public JsonSchemaValidationException(String message) {
		super(message);
	}

	public JsonSchemaValidationException(String message, JsonSchemaValidationResult validationResult) {
		super(message);
		this.validationResult = validationResult;
	}

	public JsonSchemaValidationException(String message, Throwable cause) {
		super(message, cause);
	}

	public JsonSchemaValidationException(String message, Throwable cause, JsonSchemaValidationResult validationResult) {
		super(message, cause);
		this.validationResult = validationResult;
	}

	public JsonSchemaValidationResult getValidationResult() {
		return validationResult;
	}
}
