package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

import java.util.List;

public class CheckErrorFromTokenEndpointResponseErrorInvalidClientOrInvalidRequest extends AbstractCondition {

	private static final List<String> EXPECTED_VALUES = ImmutableList.of("invalid_request", "invalid_client");

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {

		if (!env.containsObject("token_endpoint_response")) {
			throw error("Couldn't find token endpoint response");
		}

		String error = env.getString("token_endpoint_response", "error");

		if (Strings.isNullOrEmpty(error)) {
			throw error("Couldn't find error field");
		}

		if (!EXPECTED_VALUES.contains(error)) {
			throw error("'error' field has unexpected value", args("expected", EXPECTED_VALUES, "actual", error));
		}

		logSuccess("Token endpoint returned an expected error", args("expected", EXPECTED_VALUES, "actual", error));

		return env;
	}
}
