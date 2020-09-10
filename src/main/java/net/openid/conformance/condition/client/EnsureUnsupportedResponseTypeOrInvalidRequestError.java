package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

/**
 * Check to make sure a "unsupported_response_type" or "invalid_request" error was received from the server
 */
public class EnsureUnsupportedResponseTypeOrInvalidRequestError extends AbstractCondition {

	private static final List<String> PERMITTED_ERRORS = ImmutableList.of("invalid_request", "unsupported_response_type");

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {

		String error = env.getString("authorization_endpoint_response", "error");

		if (Strings.isNullOrEmpty(error)) {
			throw error("Expected 'error' field is missing from authorization endpoint response");
		}

		if (!PERMITTED_ERRORS.contains(error)) {
			throw error("authorization endpoint response 'error' field has unexpected value", args("permitted", PERMITTED_ERRORS, "actual", error));
		}

		logSuccess("Authorization endpoint returned an expected 'error'", args("permitted", PERMITTED_ERRORS, "error", error));

		return env;
	}

}
