package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class OIDSSFEnsureSecurityEventTokenDoesNotContainExpClaim extends AbstractCondition {

	@PreEnvironment(required = {"ssf"})
	@Override
	public Environment evaluate(Environment env) {

		JsonElement setTokenClaimsEl = env.getElementFromObject("ssf", "verification.token.claims");

		if (setTokenClaimsEl == null) {
			throw error("Couldn't find SET token claims");
		}

		JsonObject setTokenClaims = setTokenClaimsEl.getAsJsonObject();

		if (setTokenClaims.has("exp")) {
			throw error("SET claims must not contain the 'exp' claim", args("exp", setTokenClaims.get("exp")));
		}

		logSuccess("'exp' claim not present in SET claims");

		return env;
	}
}
