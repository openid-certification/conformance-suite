package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import com.networknt.schema.ValidationMessage;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.vci10issuer.util.JsonSchemaValidation;
import net.openid.conformance.vci10issuer.util.JsonSchemaValidationInput;
import net.openid.conformance.vci10issuer.util.JsonSchemaValidationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Base class for conditions that check for unknown/additional properties in schema-validated JSON.
 * Filters validation messages to only report additionalProperties errors, ignoring structural
 * errors (which are handled by the main validator condition).
 */
public abstract class AbstractCheckForUnexpectedSchemaProperties extends AbstractJsonSchemaBasedValidation {

	@Override
	protected void onValidationFailure(Environment env, JsonSchemaValidationResult validationResult, JsonSchemaValidationInput input) {
		Set<ValidationMessage> additionalPropsErrors = validationResult.getValidationMessages().stream()
			.filter(m -> "additionalProperties".equals(m.getType()))
			.collect(Collectors.toSet());
		if (!additionalPropsErrors.isEmpty()) {
			List<JsonObject> unknownProps = new ArrayList<>();
			for (ValidationMessage msg : additionalPropsErrors) {
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
