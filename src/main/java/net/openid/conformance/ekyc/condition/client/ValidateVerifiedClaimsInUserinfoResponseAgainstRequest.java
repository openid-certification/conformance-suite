package net.openid.conformance.ekyc.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateVerifiedClaimsInUserinfoResponseAgainstRequest extends AbstractValidateVerifiedClaimsAgainstRequest {

	public ValidateVerifiedClaimsInUserinfoResponseAgainstRequest() {
	}

	public ValidateVerifiedClaimsInUserinfoResponseAgainstRequest(boolean checkExpectedValuesConfiguration) {
		this.checkExpectedValuesConfiguration = checkExpectedValuesConfiguration;
	}


	@Override
	@PreEnvironment(required = {"authorization_endpoint_request", "verified_claims_response"})
	public Environment evaluate(Environment env) {
		JsonElement verifiedClaimsElementFromResponse = env.getElementFromObject("verified_claims_response", "userinfo");
		JsonElement requestedVerifiedClaimsElement = env.getElementFromObject("authorization_endpoint_request", "claims.userinfo.verified_claims");

		if(verifiedClaimsElementFromResponse==null) {
			throw error("verified_claims not found");
		}
		// expected values are in config.ekyc.expected_verified_claims keyed by sub identifier
		JsonElement expectedValuesConfig = null;
		if(checkExpectedValuesConfiguration) {
			String sub = env.getString("userinfo", "sub");
			if(!Strings.isNullOrEmpty(sub)) {
				expectedValuesConfig = env.getElementFromObject("config", "ekyc.expected_verified_claims."+sub);
			}
		}
		validateResponseAgainstRequestedVerifiedClaims(requestedVerifiedClaimsElement, verifiedClaimsElementFromResponse, expectedValuesConfig);

		logSuccess("Verified claims are valid", args("response", verifiedClaimsElementFromResponse,
			"requested", requestedVerifiedClaimsElement));
		return env;
	}

}
