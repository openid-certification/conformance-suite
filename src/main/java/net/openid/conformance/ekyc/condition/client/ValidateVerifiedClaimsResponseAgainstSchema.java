package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.validation.JsonSchemaValidationInput;
import net.openid.conformance.util.validation.JsonSchemaValidationResult;

public class ValidateVerifiedClaimsResponseAgainstSchema extends AbstractEkycSchemaBasedValidation {

	private static final String SCHEMA_RESOURCE = "json-schemas/ekyc-ida/verified_claims.json";

	@Override
	protected JsonSchemaValidationInput createJsonSchemaValidationInput(Environment env) {
		JsonObject wrappedClaims = env.getObject(EKYC_VALIDATION_INPUT_KEY);
		return new JsonSchemaValidationInput("verified_claims response", SCHEMA_RESOURCE, wrappedClaims);
	}

	@Override
	protected void onValidationSuccess(Environment env, JsonSchemaValidationInput input) {
		// Suppress default success log; evaluate() logs a more detailed message
	}

	@Override
	protected void onValidationFailure(Environment env, JsonSchemaValidationResult validationResult, JsonSchemaValidationInput input) {
		JsonSchemaValidationResult structuralErrors = validationResult.withoutUnknownPropertyErrors();
		if (!structuralErrors.isValid()) {
			super.onValidationFailure(env, structuralErrors, input);
		}
	}

	@Override
	@PreEnvironment(required = {"verified_claims_response"})
	public Environment evaluate(Environment env) {
		JsonObject claimsObject = extractAndWrapResponseClaims(env);
		if (claimsObject == null) {
			throw error("Could not find verified_claims");
		}

		JsonObject verifiedClaimsResponse = env.getObject("verified_claims_response");
		String location = verifiedClaimsResponse.has("userinfo") ? "userinfo" : "id_token";

		env.putObject(EKYC_VALIDATION_INPUT_KEY, claimsObject);
		try {
			super.evaluate(env);
		} finally {
			env.removeObject(EKYC_VALIDATION_INPUT_KEY);
		}

		logSuccess("Verified claims are valid", args("location", location, "verified_claims", claimsObject.get("verified_claims")));
		return env;
	}
}
