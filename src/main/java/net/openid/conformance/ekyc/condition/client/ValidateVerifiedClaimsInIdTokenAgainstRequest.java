package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateVerifiedClaimsInIdTokenAgainstRequest extends AbstractValidateVerifiedClaimsAgainstRequest {

	@Override
	@PreEnvironment(required = {"server", "authorization_endpoint_request", "verified_claims_response"})
	public Environment evaluate(Environment env) {
		JsonElement verifiedClaimsElementFromResponse = env.getElementFromObject("verified_claims_response", "id_token");
		JsonElement requestedVerifiedClaimsElement = env.getElementFromObject("authorization_endpoint_request", "claims.id_token.verified_claims");

		if(verifiedClaimsElementFromResponse==null) {
			throw error("verified_claims not found");
		}
		validateResponseAgainstRequestedVerifiedClaims(requestedVerifiedClaimsElement, verifiedClaimsElementFromResponse);

		logSuccess("Verified claims are valid", args("response", verifiedClaimsElementFromResponse,
			"requested", requestedVerifiedClaimsElement));
		return env;
	}

}
