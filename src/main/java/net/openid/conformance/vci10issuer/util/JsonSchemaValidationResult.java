package net.openid.conformance.vci10issuer.util;

import com.google.gson.JsonObject;
import com.networknt.schema.ValidationMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

	public JsonSchemaValidationResult withoutAdditionalPropertiesErrors() {
		Set<ValidationMessage> filtered = validationMessages.stream()
			.filter(m -> !"additionalProperties".equals(m.getType()))
			.collect(Collectors.toSet());
		return new JsonSchemaValidationResult(filtered);
	}

	public JsonSchemaValidationResult onlyAdditionalPropertiesErrors() {
		Set<ValidationMessage> filtered = validationMessages.stream()
			.filter(m -> "additionalProperties".equals(m.getType()))
			.collect(Collectors.toSet());
		return new JsonSchemaValidationResult(filtered);
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
