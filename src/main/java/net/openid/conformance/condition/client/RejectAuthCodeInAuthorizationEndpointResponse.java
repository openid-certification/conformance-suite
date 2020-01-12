package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class RejectAuthCodeInAuthorizationEndpointResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "callback_query_params", "callback_params"} )
	public Environment evaluate(Environment env) {
		if (env.getString("callback_query_params", "code") != null) {
			throw error("Authorization code is present in URL query returned from but an error was expected");
		}

		if (env.getString("callback_params", "code") != null) {
			throw error("Authorization code is present in URL fragment returned from but an error was expected");
		}

		logSuccess("Authorization code is not present in authorization endpoint response");
		return env;
	}

}
