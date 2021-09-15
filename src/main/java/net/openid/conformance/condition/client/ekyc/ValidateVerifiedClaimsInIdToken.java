package net.openid.conformance.condition.client.ekyc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateVerifiedClaimsInIdToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"server", "authorization_endpoint_request", "verified_claims_response"})
	public Environment evaluate(Environment env) {
		JsonElement verifiedClaimsElement = env.getElementFromObject("verified_claims_response", "userinfo");
		//TODO validate against what was requested in authorization_endpoint_request
		logSuccess("Verified claims are valid", args("verified_claims", verifiedClaimsElement));
		return env;
	}

}
