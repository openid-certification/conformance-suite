package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AustraliaConnectIdEnsureRequestObjectContainsNoAcrClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_request_object")
	public Environment evaluate(Environment env) {

		JsonElement acrClaim = env.getElementFromObject("authorization_request_object", "claims.claims.id_token.acr");

		if (acrClaim != null) {
			throw error("Request object claims contain ID Token acr request", args("acr", acrClaim));
		}

		JsonElement acrValues = env.getElementFromObject("authorization_request_object", "claims.acr_values");
		if (acrValues != null) {
			throw error("Request object contains acr_values", args("acr_values", acrValues));
		}

		logSuccess("Request object does not contain any acr requests");
		return env;
	}
}
