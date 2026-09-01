package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonObject;
import com.networknt.schema.SchemaRegistry;
import net.openid.conformance.condition.AbstractCheckForUnexpectedSchemaProperties;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.validation.JsonSchemaValidationInput;

import java.util.function.Consumer;

public class CheckForUnexpectedPropertiesInVerifiedClaimsRequest extends AbstractCheckForUnexpectedSchemaProperties {

	private static final String SCHEMA_RESOURCE = "json-schemas/ekyc-ida/verified_claims_request.json";

	@Override
	protected JsonSchemaValidationInput createJsonSchemaValidationInput(Environment env) {
		JsonObject authorizationRequest = env.getObject("authorization_endpoint_request");
		JsonObject claims = authorizationRequest.has("claims") ? authorizationRequest.getAsJsonObject("claims") : null;
		return new JsonSchemaValidationInput("verified_claims request", SCHEMA_RESOURCE, claims);
	}

	@Override
	protected Consumer<SchemaRegistry.Builder> schemaBuilderCustomizer() {
		return AbstractEkycSchemaBasedValidation.ekycSchemaMapperCustomizer();
	}

	@Override
	@PreEnvironment(required = {"authorization_endpoint_request"})
	public Environment evaluate(Environment env) {
		JsonObject authorizationRequest = env.getObject("authorization_endpoint_request");
		if (!authorizationRequest.has("claims")) {
			logSuccess("No claims to check for unexpected properties");
			return env;
		}
		if (!authorizationRequest.get("claims").isJsonObject()) {
			// Not an unknown-property matter, and this condition runs with WARNING:
			// ValidateVerifiedClaimsRequestAgainstSchema reports this at FAILURE.
			log("The claims member of the authorization request is not a JSON object, so it cannot be checked for unexpected properties",
				args("claims", authorizationRequest.get("claims")));
			return env;
		}
		return super.evaluate(env);
	}
}
