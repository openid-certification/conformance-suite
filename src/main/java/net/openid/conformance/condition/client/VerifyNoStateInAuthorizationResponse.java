package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

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
