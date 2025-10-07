package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.time.Instant;

public class OIDSSFEnsureSecurityEventTokenIatIsNotInFuture extends AbstractCondition {

	@PreEnvironment(required = {"set_token"})
	@Override
	public Environment evaluate(Environment env) {

		JsonElement setTokenClaimsEl = env.getElementFromObject("set_token", "claims");

		if (setTokenClaimsEl == null) {
			throw error("Couldn't find SET token claims");
		}

		JsonObject setTokenClaims = setTokenClaimsEl.getAsJsonObject();
		if (!setTokenClaims.has("iat")) {
			throw error("iat claim is missing. SET claims must contain the 'iat' claim");
		}

		long iat = OIDFJSON.getLong(setTokenClaims.get("iat"));
		Instant iatInstant = Instant.ofEpochSecond(iat);
		Instant now = Instant.now();
		if (now.isBefore(iatInstant)) {
			throw error("SET contains 'iat' in the future", args("iat", iatInstant, "now", now));
		}

		logSuccess("Valid iat claim present in SET claims", args("iat", iat));

		return env;
	}
}
