package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureInvalidRequestUriError extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {

		String error = env.getString("authorization_endpoint_response", "error");
		String expected = "invalid_request_uri";

		if (Strings.isNullOrEmpty(error)) {
			throw error("Expected 'error' field not found");
		} else if (!error.equals(expected)) {
			throw error("'error' field has unexpected value", args("expected", expected, "actual", error));
		} else {
			logSuccess("Authorization endpoint returned expected 'error' of '"+expected+"'", args("error", error));

			return env;
		}
	}
}
