package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

import java.util.List;

public class CheckErrorFromAuthorizationEndpointErrorInvalidRequestOrInvalidRequestObject extends AbstractCondition {

	private static final List<String> EXPECTED_VALUES = ImmutableList.of("invalid_request_object", "invalid_request");

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {

		String error = env.getString("authorization_endpoint_response", "error");

		if (Strings.isNullOrEmpty(error)) {
			throw error("Expected 'error' field not found");
		} else if (!EXPECTED_VALUES.contains(error)) {
			throw error("'error' field has unexpected value", args("expected", EXPECTED_VALUES, "actual", error));
		} else {
			logSuccess("Authorization endpoint returned expected error", args("expected", EXPECTED_VALUES, "actual", error));
			return env;
		}
	}
}