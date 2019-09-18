package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class VerifyNoStateInAuthorizationResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {

		String state = env.getString("authorization_endpoint_response", "state");

		if (!Strings.isNullOrEmpty(state)) {
			throw error("state has been returned in authorization endpoint response, when it shouldn't have been");
		}

		logSuccess("Authorization endpoint response is correctly missing 'state'");

		return env;
	}

}
