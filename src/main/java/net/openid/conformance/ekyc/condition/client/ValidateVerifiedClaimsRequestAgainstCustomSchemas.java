package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateVerifiedClaimsRequestAgainstCustomSchemas extends AbstractValidateVerifiedClaimsAgainstCustomSchemas {

	@Override
	@PreEnvironment(required = {"authorization_endpoint_request"})
	public Environment evaluate(Environment env) {
		JsonObject authorizationRequest = env.getObject("authorization_endpoint_request");
		if (!authorizationRequest.has("claims")) {
			logSuccess("No claims to validate against custom schemas");
			return env;
		}

		JsonElement requestSchemas = env.getElementFromObject("config", "ekyc.request_schemas");
		if (requestSchemas == null) {
			logSuccess("No custom request schemas configured");
			return env;
		}

		if (!authorizationRequest.get("claims").isJsonObject()) {
			throw error("The claims member of the authorization request is not a JSON object",
				args("claims", authorizationRequest.get("claims")));
		}
		JsonObject claims = authorizationRequest.getAsJsonObject("claims");
		for (JsonElement schemaElement : OIDFJSON.packJsonElementIntoJsonArray(requestSchemas)) {
			validateAgainstCustomSchema(claims, schemaElement, "eKYC Additional Request Validation Schemas");
		}

		logSuccess("Validated request claims against custom schemas");
		return env;
	}
}
