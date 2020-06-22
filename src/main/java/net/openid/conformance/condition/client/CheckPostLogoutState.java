package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckPostLogoutState extends AbstractCondition {

	@Override
	@PreEnvironment(required = "post_logout_redirect", strings = "end_session_state")
	public Environment evaluate(Environment env) {
		String state = env.getString("post_logout_redirect", "query_string_params.state");
		String expectedState = env.getString("end_session_state");
		if (state == null) {
			throw error("state not present in query params passed to post logout redirect uri.");
		}

		if (!expectedState.equals(state)) {
			throw error("state in query params passed to post logout redirect uri does not match state passed in the end_session request.",
				args("actual", state, "expected", expectedState));
		}

		logSuccess("state passed to post logout redirect uri matches request");

		return env;

	}

}
