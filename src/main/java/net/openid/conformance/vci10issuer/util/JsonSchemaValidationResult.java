package net.openid.conformance.vci10issuer.util;

import com.google.gson.JsonObject;
import com.networknt.schema.ValidationMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class JsonSchemaValidationResult {

	private final Set<ValidationMessage> validationMessages;

	public JsonSchemaValidationResult(Set<ValidationMessage> validationMessages) {
		this.validationMessages = validationMessages;
	}

	public boolean isValid() {
		return validationMessages.isEmpty();
	}

	public Set<ValidationMessage> getValidationMessages() {
		return validationMessages;
	}

	public List<JsonObject> getPropertyErrors() {
		List<JsonObject> propertyErrorsWithPaths = new ArrayList<>();
		for (ValidationMessage error : validationMessages) {
			JsonObject propertyError = new JsonObject();
			propertyError.addProperty("error", error.getError());
			if (error.getProperty() != null) {
				propertyError.addProperty("property", error.getProperty());
			}
			propertyError.addProperty("path", JsonSchemaValidation.toInstancePropertyPath(error.getInstanceLocation(), error.getProperty()));
			propertyErrorsWithPaths.add(propertyError);
		}
		return propertyErrorsWithPaths;
	}
}
