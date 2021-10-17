package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ReverseScopeOrderInDynamicRegistrationEndpointRequest extends AbstractReverseScopeOrder {
	public static final String envKey = "dynamic_registration_request";

	@Override
	@PreEnvironment(required = envKey)
	@PostEnvironment(required = envKey)
	public Environment evaluate(Environment env) {

		reverseScope(env, envKey);

		return env;

	}

}
