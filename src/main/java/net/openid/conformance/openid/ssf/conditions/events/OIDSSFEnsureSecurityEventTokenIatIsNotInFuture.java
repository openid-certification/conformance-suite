package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;

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

		try {
			// Shared RFC 7519 iat validation: a plausible unix-seconds timestamp (catches
			// iat: 0 and millisecond values) that is not in the future beyond a 5-minute
			// clock-skew tolerance - a transmitter whose clock is a second ahead of the
			// suite's must not fail certification.
			JWTUtil.validateIatClaim(iat);
		} catch (IllegalArgumentException e) {
			throw error("SET contains an invalid 'iat' claim: " + e.getMessage(), args("iat", iat));
		}

		logSuccess("Valid iat claim present in SET claims", args("iat", iat));

		return env;
	}
}
