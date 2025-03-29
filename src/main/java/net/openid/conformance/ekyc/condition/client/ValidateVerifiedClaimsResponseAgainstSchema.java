package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateVerifiedClaimsResponseAgainstSchema extends AbstractValidateAgainstSchema {

	@Override
	@PreEnvironment(required = {"verified_claims_response"})
	public Environment evaluate(Environment env) {
		JsonObject verifiedClaimsResponse = env.getObject("verified_claims_response");
		JsonElement claimsElement = null;
		String location = "";
		//TODO I assumed id_token will be processed before userinfo so if we have userinfo then just process it
		// otherwise process id_token
		if(verifiedClaimsResponse.has("userinfo")) {
			claimsElement = verifiedClaimsResponse.get("userinfo");
			location = "userinfo";
		} else {
			claimsElement = verifiedClaimsResponse.get("id_token");
			location = "id_token";
		}
		if(claimsElement==null) {
			throw error("Could not find verified_claims");
		}
		//we add the outer {"verified_claims":...} here
		JsonObject claimsObject = new JsonObject();
		claimsObject.add("verified_claims", claimsElement);

		// Verify against eKYC schema resource file verified_claims.json
		JsonElement eKYCVerifiedClaimsSchema = getJsonElementFromResourceFile(ekycVerifiedClaimsResourceFile);
		peformSchemaValidation("verified_claims response", claimsObject, "eKYC verified_claims", eKYCVerifiedClaimsSchema);

		// Verify user configured response schemas
		JsonElement responseSchemas = env.getElementFromObject("config", "ekyc.response_schemas");
		for(JsonElement responseSchemaElement : OIDFJSON.packJsonElementIntoJsonArray(responseSchemas)) {
			peformSchemaValidation("verified_claims response", claimsObject, "ekyc user response_schemas", responseSchemaElement);
		}
		logSuccess("Verified claims are valid", args("location", location, "verified_claims", claimsElement));
		return env;
	}

}
