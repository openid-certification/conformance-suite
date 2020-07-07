package net.openid.conformance.condition.client;

import com.google.common.base.Strings;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Checks to make sure the "state" parameter matches the one that was saved previously.
 */
public class CheckMatchingStateParameter extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {
		if (!env.containsObject("authorization_endpoint_response")) {
			throw error("Couldn't find callback parameters");
		}

		String expected = env.getString("state");
		String actual = env.getString("authorization_endpoint_response", "state");

		if (Strings.isNullOrEmpty(expected)) {
			// we didn't save a 'state' value, we need to make sure one wasn't returned
			if (Strings.isNullOrEmpty(actual)) {
				// we're good
				logSuccess("No state parameter to check");
				return env;
			} else {
				throw error("No state value was sent, but a state parameter was returned", args("expected", Strings.nullToEmpty(expected), "actual", Strings.nullToEmpty(actual)));
			}
		} else {
			// we did save a state parameter, make sure it's the same as before
			if (Strings.isNullOrEmpty(actual)) {
				throw error("A state value was sent, but no state parameter was returned by the authorization server", args("expected", Strings.nullToEmpty(expected), "actual", Strings.nullToEmpty(actual)));
			}
			if (expected.equals(actual)) {
				// we're good
				logSuccess("State parameter correctly returned",
					args("state", Strings.nullToEmpty(actual)));

				return env;
			} else {
				throw error("State parameter did not match",  args("expected", Strings.nullToEmpty(expected), "actual", Strings.nullToEmpty(actual)));
			}
		}

	}

}
