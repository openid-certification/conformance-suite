package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * Check to make sure a "unsupported_response_type" error was received from the server
 */
public class EnsureUnsupportedResponseTypeErrorFromAuthorizationEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment in) {
		final String expected = "unsupported_response_type";

		final String actual = in.getString("authorization_endpoint_response", "error");
		if (Strings.isNullOrEmpty(actual)) {
			throw error("No unsupported_response_type error found from the authorization endpoint", in.getObject("authorization_endpoint_response"));
		}

		if (actual.equals(expected)){
			logSuccess(expected+" error from the authorization endpoint");
			return in;
		} else {
			throw error("Incorrect error from the authorization endpoint",
				args("expected", expected, "actual", actual));
		}
	}

}
