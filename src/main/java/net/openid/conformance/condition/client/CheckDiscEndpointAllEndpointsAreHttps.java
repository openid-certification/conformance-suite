package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckDiscEndpointAllEndpointsAreHttps extends AbstractJsonUriIsValidAndHttps {

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		var discoveryDoc = env.getObject("server");

		for (String key : discoveryDoc.keySet()) {
			if (key.endsWith("_endpoint")) {
				env = validate(env, key);
			}
		}
		return env;

	}
}
