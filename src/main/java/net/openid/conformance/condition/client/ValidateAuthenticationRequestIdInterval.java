package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateAuthenticationRequestIdInterval extends AbstractCondition {

	private final double maximumInterval = 6 * 60 * 60; // 6 hours as 21600 seconds

	@Override
	@PreEnvironment(required = "backchannel_authentication_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonElement jInterval = env.getElementFromObject("backchannel_authentication_endpoint_response", "interval");

		if (jInterval == null) {
			log("interval is empty.");
			return env;
		}

		if (!jInterval.isJsonPrimitive()) {
			throw error("interval is not a primitive!");
		}

		if (!jInterval.getAsJsonPrimitive().isNumber()) {
			throw error("interval is not a number!");
		}

		int interval = OIDFJSON.getInt(jInterval);
		if (interval < 0) {
			throw error("interval is less than zero");
		}

		if (interval > maximumInterval) {
			throw error("internal is intended to be a few minutes in most cases anything over 6 hours can be seen as unreasonable.", args("expected", maximumInterval, "actual", interval));
		}

		logSuccess("interval passed all validation checks", args("interval", interval));

		return env;
	}
}
