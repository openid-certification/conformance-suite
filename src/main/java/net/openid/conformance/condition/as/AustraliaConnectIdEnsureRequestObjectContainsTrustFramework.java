package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AustraliaConnectIdEnsureRequestObjectContainsTrustFramework extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_request_object")
	public Environment evaluate(Environment env) {

		JsonElement verifiedClaimsEl = env.getElementFromObject("authorization_request_object", "claims.claims.id_token.verified_claims");

		if (verifiedClaimsEl == null) {
			logSuccess("No verified_claims requested in request object.");
			return env;
		}

		JsonElement trustFrameworkEl = env.getElementFromObject("authorization_request_object", "claims.claims.id_token.verified_claims.verification.trust_framework.value");

		if (trustFrameworkEl == null || !trustFrameworkEl.isJsonPrimitive() || !trustFrameworkEl.getAsJsonPrimitive().isString()) {
			throw error("verified_claims requested but trust_framework is not present or invalid.");
		}

		String trustFramework = OIDFJSON.getString(trustFrameworkEl);
		if (!"au_connectid".equals(trustFramework)) {
			throw error("trust_framework must be 'au_connectid'", args("actual", trustFramework));
		}

		logSuccess("trust_framework 'au_connectid' is present in request object verified_claims verification.");
		return env;
	}
}
