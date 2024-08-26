package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckNonceInBindingJwt extends AbstractCondition {

	@Override
	@PreEnvironment(required = "sdjwt")
	public Environment evaluate(Environment env) {

		String actual = env.getString("sdjwt", "binding.claims.nonce");
		String expected = env.getString("nonce");

		if (Strings.isNullOrEmpty(expected)) {
			// we didn't save a 'nonce' value, we need to make sure one wasn't returned
			if (Strings.isNullOrEmpty(actual)) {
				// we're good
				logSuccess("No nonce in response to check");
				return env;
			} else {
				throw error("No nonce value was sent, but a nonce in response was returned", args("expected", Strings.nullToEmpty(expected), "actual", Strings.nullToEmpty(actual)));
			}
		} else {

			String error = env.getString("authorization_endpoint_response", "error");

			// we did save a nonce parameter, make sure it's the same as before
			if (expected.equals(actual)) {
				// we're good
				logSuccess("nonce in response correctly returned", args("nonce", actual));

				return env;
			} else if (Strings.isNullOrEmpty(actual) && !Strings.isNullOrEmpty(error) && error.equals("invalid_request_object")) {

				logSuccess("nonce is missing from response; this is permitted when the returned error is 'invalid_request_object' and the nonce was contained in the request object");

				return env;
			} else if (Strings.isNullOrEmpty(actual)) {
					throw error("nonce was passed in request, but is missing from response (or returned in the wrong place)",  args("expected", Strings.nullToEmpty(expected), "actual", Strings.nullToEmpty(actual)));
			} else {
				throw error("nonce in response did not match",  args("expected", Strings.nullToEmpty(expected), "actual", Strings.nullToEmpty(actual)));
			}
		}
	}
}
