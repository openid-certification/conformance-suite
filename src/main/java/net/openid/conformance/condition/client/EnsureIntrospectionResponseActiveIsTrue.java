package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureIntrospectionResponseActiveIsTrue extends AbstractCondition {

	@Override
	@PreEnvironment(required = CallTokenIntrospectionEndpoint.RESPONSE_KEY)
	public Environment evaluate(Environment env) {

		Boolean active = env.getBoolean(CallTokenIntrospectionEndpoint.RESPONSE_KEY, "body_json.active");

		if (active == null) {
			throw error("active is missing from the introspection response or is not a boolean",
				args("introspection_response", env.getElementFromObject(CallTokenIntrospectionEndpoint.RESPONSE_KEY, "body_json")));
		}

		if (!active) {
			throw error("The introspection response reports the token as inactive");
		}

		logSuccess("The introspection response reports the token as active");
		return env;
	}

}
