package net.openid.conformance.ekyc.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateVerifiedClaimsInIdTokenAgainstRequest extends AbstractValidateVerifiedClaimsAgainstRequest {

	@Override
	@PreEnvironment(required = {"server", "authorization_endpoint_request", "verified_claims_response"})
	public Environment evaluate(Environment env) {



		JsonElement verifiedClaimsElementFromResponse = env.getElementFromObject("verified_claims_response", "id_token");
		JsonElement requestedVerifiedClaimsElement = env.getElementFromObject("authorization_endpoint_request", "claims.id_token.verified_claims");

		// expected values are in config.ekyc.expected_verified_claims keyed by sub identifier
		JsonElement expectedValuesConfig = null;
		if(checkExpectedValuesConfiguration) {
			String sub = env.getString("id_token", "claims.sub");
			if(!Strings.isNullOrEmpty(sub)) {
				expectedValuesConfig = env.getElementFromObject("config", "ekyc.expected_verified_claims."+sub);
			}
		}

		if(verifiedClaimsElementFromResponse==null) {
			throw error("verified_claims not found");
		}
		validateResponseAgainstRequestedVerifiedClaims(requestedVerifiedClaimsElement, verifiedClaimsElementFromResponse, expectedValuesConfig);

		logSuccess("Verified claims are valid", args("response", verifiedClaimsElementFromResponse,
			"requested", requestedVerifiedClaimsElement));
		return env;
	}

}
