package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateVerifiedClaimsRequestAgainstSchema extends AbstractValidateAgainstSchema {

	@Override
	@PreEnvironment(required = {"authorization_endpoint_request"})
	public Environment evaluate(Environment env) {
		JsonObject authorizationRequest = env.getObject("authorization_endpoint_request");
		if(authorizationRequest.has("claims")) {
			JsonElement claimsElement = authorizationRequest.getAsJsonObject("claims");

			// Verify against eKYC schema resource file verified_claims_request.json
			JsonElement eKYCVerifiedClaimsRequestSchema = getJsonElementFromResourceFile(ekycVerifiedClaimsRequestResourceFile);
			peformSchemaValidation("verified_claims request", claimsElement, "eKYC verified_claims_request ", eKYCVerifiedClaimsRequestSchema);

			// Verify user configured request schemas
			JsonElement requestSchemas = env.getElementFromObject("config", "ekyc.request_schemas");
			for(JsonElement requestSchemaElement : OIDFJSON.packJsonElementIntoJsonArray(requestSchemas)) {
				peformSchemaValidation("verified_claims request", claimsElement, "ekyc user request_schemas", requestSchemaElement);
			}
		} else {
			log("authorization_endpoint_request does not contain claims object");
		}
		logSuccess("Validated request claims against request schema");
		return env;
	}

}
