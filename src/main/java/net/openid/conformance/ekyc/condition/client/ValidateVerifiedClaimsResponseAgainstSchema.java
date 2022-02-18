package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.networknt.schema.ValidationMessage;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.io.IOException;
import java.util.Set;

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
		String claimsJson = "{\"verified_claims\":" + claimsElement.toString() + "}";
		try {
			Set<ValidationMessage> errors = checkResponseSchema(claimsJson);
			if (!errors.isEmpty()) {
				JsonArray jsonErrors = new JsonArray();
				for (ValidationMessage error: errors) {
					jsonErrors.add(error.toString());
				}
				throw error("Failed to validate verified_claims against schema",
					args("verified_claims", JsonParser.parseString(claimsJson),
						"errors", jsonErrors));
			}
		} catch (IOException e) {
			throw error("Failed to parse JSON", e);
		}

		logSuccess("Verified claims are valid", args("location", location, "verified_claims", claimsElement));
		return env;
	}

}
