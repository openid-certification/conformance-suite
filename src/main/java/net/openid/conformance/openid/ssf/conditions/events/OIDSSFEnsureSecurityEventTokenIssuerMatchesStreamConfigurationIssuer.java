package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDSSFEnsureSecurityEventTokenIssuerMatchesStreamConfigurationIssuer extends AbstractCondition {

	@PreEnvironment(required = {"set_token"})
	@Override
	public Environment evaluate(Environment env) {

		JsonElement setTokenClaimsEl = env.getElementFromObject("set_token", "claims");

		if (setTokenClaimsEl == null) {
			throw error("Couldn't find SET token claims");
		}

		JsonObject setTokenClaims = setTokenClaimsEl.getAsJsonObject();

		JsonElement issEl = setTokenClaims.get("iss");
		if (issEl == null) {
			throw error("Couldn't find iss claim");
		}

		String ssfConfigIssuer = env.getString("ssf","transmitter_metadata.issuer");
		String setIssuer = OIDFJSON.getString(issEl);

		if (!ssfConfigIssuer.equals(setIssuer)) {
			throw error("Invalid value for iss claim '"+setIssuer+"'. Should be '" + ssfConfigIssuer + "'", args("actual_iss", setIssuer, "expected_iss", ssfConfigIssuer));
		}

		logSuccess("Valid iss claim present in SET claims", args("iss", setIssuer));

		return env;
	}
}
