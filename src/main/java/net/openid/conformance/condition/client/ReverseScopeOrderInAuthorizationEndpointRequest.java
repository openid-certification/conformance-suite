package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ReverseScopeOrderInAuthorizationEndpointRequest extends AbstractReverseScopeOrder {
	public static final String envKey = "authorization_endpoint_request";

	@Override
	@PreEnvironment(required = envKey)
	@PostEnvironment(required = envKey)
	public Environment evaluate(Environment env) {

		reverseScope(env, envKey);

		return env;

	}

}
