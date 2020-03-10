package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckStateInAuthorizationResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {

		String actual = env.getString("authorization_endpoint_response", "state");
		String expected = env.getString("state");

		if (Strings.isNullOrEmpty(expected)) {
			// we didn't save a 'state' value, we need to make sure one wasn't returned
			if (Strings.isNullOrEmpty(actual)) {
				// we're good
				logSuccess("No state in response to check");
				return env;
			} else {
				throw error("No state value was sent, but a state in response was returned", args("expected", Strings.nullToEmpty(expected), "actual", Strings.nullToEmpty(actual)));
			}
		} else {

			String error = env.getString("authorization_endpoint_response", "error");

			// we did save a state parameter, make sure it's the same as before
			if (expected.equals(actual)) {
				// we're good
				logSuccess("State in response correctly returned", args("state", actual));

				return env;
			} else if (Strings.isNullOrEmpty(actual) && !Strings.isNullOrEmpty(error) && error.equals("invalid_request_object")) {

				logSuccess("State is missing from response; this is permitted when the returned error is 'invalid_request_object' and the state was contained in the request object");

				return env;
			} else if (Strings.isNullOrEmpty(actual)) {
					throw error("State was passed in request, but is missing from response (or returned in the wrong place)",  args("expected", Strings.nullToEmpty(expected), "actual", Strings.nullToEmpty(actual)));
			} else {
				throw error("State in response did not match",  args("expected", Strings.nullToEmpty(expected), "actual", Strings.nullToEmpty(actual)));
			}
		}
	}
}
