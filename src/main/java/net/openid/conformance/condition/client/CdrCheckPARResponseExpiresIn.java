package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CdrCheckPARResponseExpiresIn extends AbstractCondition {
	@Override
	@PreEnvironment(required = CallPAREndpoint.RESPONSE_KEY)
	public Environment evaluate(Environment env) {
		Long expiresIn = env.getLong(CallPAREndpoint.RESPONSE_KEY, "body_json.expires_in");
		if (expiresIn == null) {
			throw error("expires_in is missing or empty in pushed authorization response");
		}
		// "The Request URI MUST expire between 10 seconds and 90 seconds."
		if (expiresIn < 10 || expiresIn > 90) {
			throw error("CDR requires the request_uri to expire between 10 seconds and 90 seconds, but expires_in is outside that range",
				args("expires_in", expiresIn));
		}

		logSuccess("expires_in is between 10 and 90 seconds", args("expires_in", expiresIn));
		return env;
	}
}
