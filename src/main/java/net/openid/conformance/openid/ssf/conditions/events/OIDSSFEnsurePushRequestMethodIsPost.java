package net.openid.conformance.openid.ssf.conditions.events;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * RFC 8935 2.1: "The SET Transmitter makes an HTTP POST request to a pre-arranged endpoint".
 * Any other method is not a SET delivery.
 */
public class OIDSSFEnsurePushRequestMethodIsPost extends AbstractCondition {

	@Override
	@PreEnvironment(required = "ssf")
	public Environment evaluate(Environment env) {

		String method = env.getString("ssf", "push_request.method");
		if (!"POST".equals(method)) {
			throw error("Push delivery request did not use the HTTP POST method", args("method", method));
		}

		logSuccess("Push delivery request used the HTTP POST method");

		return env;
	}
}
