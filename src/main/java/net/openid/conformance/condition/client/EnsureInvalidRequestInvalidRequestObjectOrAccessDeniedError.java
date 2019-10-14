package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class EnsureInvalidRequestInvalidRequestObjectOrAccessDeniedError extends AbstractCondition {

	private static final List<String> PERMITTED_ERRORS = ImmutableList.of("invalid_request", "invalid_request_object", "access_denied");

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {

		String error = env.getString("authorization_endpoint_response", "error");

		if (Strings.isNullOrEmpty(error)) {
			throw error("Permitted 'error' field not found");
		}

		if (!PERMITTED_ERRORS.contains(error)) {
			throw error("'error' field has unexpected value", args("permitted", PERMITTED_ERRORS, "actual", error));
		}

		logSuccess("Authorization endpoint returned 'error'", args("permitted", PERMITTED_ERRORS, "error", error));

		return env;
	}
}
