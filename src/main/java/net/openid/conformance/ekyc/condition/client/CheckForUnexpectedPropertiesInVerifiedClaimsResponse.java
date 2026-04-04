package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonObject;
import com.networknt.schema.ValidationMessage;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.validation.JsonSchemaValidation;
import net.openid.conformance.util.validation.JsonSchemaValidationInput;
import net.openid.conformance.util.validation.JsonSchemaValidationResult;

import java.util.ArrayList;
import java.util.List;

public class CheckForUnexpectedPropertiesInVerifiedClaimsResponse extends AbstractEkycSchemaBasedValidation {

	private static final String SCHEMA_RESOURCE = "json-schemas/ekyc-ida/verified_claims.json";

	@Override
	protected JsonSchemaValidationInput createJsonSchemaValidationInput(Environment env) {
		JsonObject wrappedClaims = env.getObject(EKYC_VALIDATION_INPUT_KEY);
		return new JsonSchemaValidationInput("verified_claims response", SCHEMA_RESOURCE, wrappedClaims);
	}

	@Override
	protected void onValidationFailure(Environment env, JsonSchemaValidationResult validationResult, JsonSchemaValidationInput input) {
		JsonSchemaValidationResult additionalPropsResult = validationResult.onlyAdditionalPropertiesErrors();
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

	@Override
	@PreEnvironment(required = {"verified_claims_response"})
	public Environment evaluate(Environment env) {
		JsonObject claimsObject = extractAndWrapResponseClaims(env);
		if (claimsObject == null) {
			logSuccess("No verified claims to check for unexpected properties");
			return env;
		}

		env.putObject(EKYC_VALIDATION_INPUT_KEY, claimsObject);
		try {
			return super.evaluate(env);
		} finally {
			env.removeObject(EKYC_VALIDATION_INPUT_KEY);
		}
	}
}
