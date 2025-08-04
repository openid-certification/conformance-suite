package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.vci10issuer.util.JsonSchemaValidation;
import net.openid.conformance.vci10issuer.util.JsonSchemaValidation.JsonSchemaValidationResult;
import net.openid.conformance.vci10issuer.util.JsonSchemaValidationInput;

import java.io.IOException;

public abstract class AbstractJsonSchemaBasedValidation extends AbstractCondition {

	protected abstract JsonSchemaValidationInput createJsonSchemaValidationInput(Environment env);

	@Override
	public Environment evaluate(Environment env) {

		JsonSchemaValidationInput input = createJsonSchemaValidationInput(env);

		JsonObject inputJsonObject = input.getJsonObject();
		if (inputJsonObject == null) {
			throw error(String.format("%s input object not found", input.getInputName()));
		}

		JsonSchemaValidation jsonSchemaValidation = createJsonSchemaValidation(input);
		try {
			JsonSchemaValidationResult validationResult = jsonSchemaValidation.validate(inputJsonObject);
			if (!validationResult.isValid()) {
				onValidationFailure(validationResult, input);
			}
			onValidationSuccess(input);
		} catch (IOException e) {
			throw new RuntimeException("JSON Schema based input validation failed", e);
		}

		return env;
	}

	protected void onValidationSuccess(JsonSchemaValidationInput input) {
		logSuccess(String.format("%s input is valid", input.getInputName()), args("input", input.getJsonObject()));
	}

	protected void onValidationFailure(JsonSchemaValidationResult validationResult, JsonSchemaValidationInput input) {
		throw error(String.format("Found invalid entries in %s input", input.getInputName()), args("invalid_entries", validationResult.getPropertyErrors(), "input", input.getJsonObject()));
	}

	protected JsonSchemaValidation createJsonSchemaValidation(JsonSchemaValidationInput input) {
		return new JsonSchemaValidation(input.getSchemaResource());
	}

}
