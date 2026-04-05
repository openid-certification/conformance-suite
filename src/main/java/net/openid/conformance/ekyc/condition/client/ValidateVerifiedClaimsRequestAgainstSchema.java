package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.validation.JsonSchemaValidationInput;

public class ValidateVerifiedClaimsRequestAgainstSchema extends AbstractEkycSchemaBasedValidation {

	private static final String SCHEMA_RESOURCE = "json-schemas/ekyc-ida/verified_claims_request.json";

	@Override
	protected JsonSchemaValidationInput createJsonSchemaValidationInput(Environment env) {
		JsonObject authorizationRequest = env.getObject("authorization_endpoint_request");
		JsonObject claims = authorizationRequest.has("claims") ? authorizationRequest.getAsJsonObject("claims") : null;
		return new JsonSchemaValidationInput("verified_claims request", SCHEMA_RESOURCE, claims);
	}

	/**
	 * Unknown properties are surfaced (as a warning) by
	 * {@link CheckForUnexpectedPropertiesInVerifiedClaimsRequest}, not by this condition, per
	 * IAVC 5.2 ("Implementations shall ignore any sub-element not defined in this specification or
	 * extensions of this specification").
	 */
	@Override
	protected boolean ignoreUnknownPropertyStrictness() {
		return true;
	}

	@Override
	@PreEnvironment(required = {"authorization_endpoint_request"})
	public Environment evaluate(Environment env) {
		JsonObject authorizationRequest = env.getObject("authorization_endpoint_request");
		if (!authorizationRequest.has("claims")) {
			logSuccess("No claims to validate against request schema");
			return env;
		}

		return super.evaluate(env);
	}
}
