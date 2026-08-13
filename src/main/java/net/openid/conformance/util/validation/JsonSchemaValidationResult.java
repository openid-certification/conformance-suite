package net.openid.conformance.util.validation;

import com.google.gson.JsonObject;
import com.networknt.schema.Error;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JsonSchemaValidationResult {

	private final List<Error> validationMessages;

	public JsonSchemaValidationResult(List<Error> validationMessages) {
		this.validationMessages = validationMessages;
	}

	public boolean isValid() {
		return validationMessages.isEmpty();
	}

	public List<Error> getValidationMessages() {
		return validationMessages;
	}

	public JsonSchemaValidationResult withoutUnknownPropertyErrors() {
		List<Error> filtered = validationMessages.stream()
			.filter(m -> !isUnknownPropertyError(m))
			.collect(Collectors.toList());
		return new JsonSchemaValidationResult(filtered);
	}

	public JsonSchemaValidationResult onlyUnknownPropertyErrors() {
		List<Error> filtered = validationMessages.stream()
			.filter(JsonSchemaValidationResult::isUnknownPropertyError)
			.collect(Collectors.toList());
		return new JsonSchemaValidationResult(filtered);
	}

	private static boolean isUnknownPropertyError(Error m) {
		String type = m.getKeyword();
		return "additionalProperties".equals(type) || "unevaluatedProperties".equals(type);
	}

	public List<JsonObject> getPropertyErrors() {
		List<JsonObject> propertyErrorsWithPaths = new ArrayList<>();
		for (Error error : validationMessages) {
			JsonObject propertyError = new JsonObject();
			propertyError.addProperty("error", error.getMessage());
			if (error.getProperty() != null) {
				propertyError.addProperty("property", error.getProperty());
			}
			propertyError.addProperty("path", JsonSchemaValidation.toInstancePropertyPath(error.getInstanceLocation(), error.getProperty()));
			propertyErrorsWithPaths.add(propertyError);
		}
		return propertyErrorsWithPaths;
	}
}
