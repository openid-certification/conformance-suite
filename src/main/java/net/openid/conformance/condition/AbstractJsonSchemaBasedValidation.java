package net.openid.conformance.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.validation.JsonSchemaValidation;
import net.openid.conformance.util.validation.JsonSchemaValidationException;
import net.openid.conformance.util.validation.JsonSchemaValidationInput;
import net.openid.conformance.util.validation.JsonSchemaValidationResult;

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
				onValidationFailure(env, validationResult, input);
			}
			onValidationSuccess(env, input);
		} catch (IOException e) {
			throw new RuntimeException("JSON Schema based input validation failed", e);
		}

		return env;
	}

	protected void onValidationSuccess(Environment env, JsonSchemaValidationInput input) {
		logSuccess(String.format("%s input is valid", input.getInputName()), args("input", input.getJsonObject(), "schema_link", "/" + input.getSchemaResource()));
	}

	protected void onValidationFailure(Environment env, JsonSchemaValidationResult validationResult, JsonSchemaValidationInput input) {
		throw error(String.format("Found invalid entries in %s input", input.getInputName()), new JsonSchemaValidationException("Schema Validation Failed", validationResult), args("invalid_entries", validationResult.getPropertyErrors(), "input", input.getJsonObject(), "schema_link", "/" + input.getSchemaResource()));
	}

	protected JsonSchemaValidation createJsonSchemaValidation(JsonSchemaValidationInput input) {
		JsonSchemaValidation validation = new JsonSchemaValidation(input.getSchemaResource());
		validation.setIgnoreUnknownPropertyStrictness(ignoreUnknownPropertyStrictness());
		return validation;
	}

	/**
	 * Structural validators (called with FAILURE) that have a paired unknown-property condition
	 * (called with WARNING) can override this to return true so that unknown properties never fail
	 * them, not even indirectly via composite oneOf/anyOf errors; see
	 * {@link JsonSchemaValidation#setIgnoreUnknownPropertyStrictness}. Before enabling this for a
	 * schema, check it has no oneOf branches discriminated only by {@code additionalProperties:
	 * false} - removing the keyword would let such a payload match more than one branch.
	 */
	protected boolean ignoreUnknownPropertyStrictness() {
		return false;
	}

}
