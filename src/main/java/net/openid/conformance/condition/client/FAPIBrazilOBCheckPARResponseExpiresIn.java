package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilOBCheckPARResponseExpiresIn extends AbstractCondition {
	@Override
	@PreEnvironment(required = CallPAREndpoint.RESPONSE_KEY)
	public Environment evaluate(Environment env) {
		Long expiresIn = env.getLong(CallPAREndpoint.RESPONSE_KEY, "body_json.expires_in");
		if (expiresIn == null) {
			throw error("expires_in is missing or empty in pushed authorization response");
		}
		// "the minimum expiration time of request_uri must be 60 seconds;"
		if (expiresIn < 60) {
			throw error("The Brazil OpenFinance profile states 'the minimum expiration time of request_uri must be 60 seconds', but expires_in is under 60 seconds",
				args("expires_in", expiresIn));
		}

		logSuccess("expires_in is more than or equal to 60 seconds",  args("expires_in", expiresIn));
		return env;
	}
}
