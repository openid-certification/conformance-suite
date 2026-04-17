package net.openid.conformance.condition;

import com.google.gson.JsonObject;
import com.networknt.schema.ValidationMessage;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.validation.JsonSchemaValidation;
import net.openid.conformance.util.validation.JsonSchemaValidationInput;
import net.openid.conformance.util.validation.JsonSchemaValidationResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for conditions that check for unknown/additional properties in schema-validated JSON.
 * Filters validation messages to only report additionalProperties errors, ignoring structural
 * errors (which are handled by the main validator condition).
 */
public abstract class AbstractCheckForUnexpectedSchemaProperties extends AbstractJsonSchemaBasedValidation {

	@Override
	protected void onValidationFailure(Environment env, JsonSchemaValidationResult validationResult, JsonSchemaValidationInput input) {
		JsonSchemaValidationResult additionalPropsResult = validationResult.onlyUnknownPropertyErrors();
		if (!additionalPropsResult.isValid()) {
			List<JsonObject> unknownProps = new ArrayList<>();
			for (ValidationMessage msg : additionalPropsResult.getValidationMessages()) {
				JsonObject entry = new JsonObject();
				entry.addProperty("property", msg.getProperty());
				entry.addProperty("path", JsonSchemaValidation.toInstancePropertyPath(msg.getInstanceLocation(), msg.getProperty()));
				unknownProps.add(entry);
			}
			throw error("Unknown properties were found in the " + input.getInputName()
					+ ". This may indicate the sender has misunderstood the spec, or it may be using extensions the test suite is unaware of.",
				args("unknown_properties", unknownProps, "input", input.getJsonObject(), "schema_link", "/" + input.getSchemaResource()));
		}
	}
}
