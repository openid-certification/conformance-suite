package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractSessionStateFromAuthorizationResponse extends AbstractCondition {
	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	@PostEnvironment(strings = "session_state")
	public Environment evaluate(Environment env) {
		String sessionState = env.getString("authorization_endpoint_response", "session_state");
		if (Strings.isNullOrEmpty(sessionState)) {
			throw error("Couldn't find session_state in authorization_endpoint_response");
		}

		env.putString("session_state", sessionState);
		logSuccess("Found session_state",
			args("session_state", sessionState));
		return env;
	}
}
