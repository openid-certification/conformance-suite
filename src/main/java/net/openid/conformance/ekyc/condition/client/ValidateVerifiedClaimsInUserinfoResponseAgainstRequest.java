package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateVerifiedClaimsInUserinfoResponseAgainstRequest extends AbstractValidateVerifiedClaimsAgainstRequest {

	@Override
	@PreEnvironment(required = {"authorization_endpoint_request", "verified_claims_response"})
	public Environment evaluate(Environment env) {
		JsonElement verifiedClaimsElementFromResponse = env.getElementFromObject("verified_claims_response", "userinfo");
		JsonElement requestedVerifiedClaimsElement = env.getElementFromObject("authorization_endpoint_request", "claims.userinfo.verified_claims");

		if(verifiedClaimsElementFromResponse==null) {
			throw error("verified_claims not found");
		}
		validateResponseAgainstRequestedVerifiedClaims(requestedVerifiedClaimsElement, verifiedClaimsElementFromResponse);

		logSuccess("Verified claims are valid", args("response", verifiedClaimsElementFromResponse,
			"requested", requestedVerifiedClaimsElement));
		return env;
	}

}
