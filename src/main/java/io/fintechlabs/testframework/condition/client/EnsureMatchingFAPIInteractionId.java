package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class EnsureMatchingFAPIInteractionId extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource_endpoint_response_headers", strings = "fapi_interaction_id")
	public Environment evaluate(Environment env) {

		// get the client ID from the configuration
		String expected = env.getString("fapi_interaction_id");
		String actual = env.getString("resource_endpoint_response_headers", "x-fapi-interaction-id");

		if (!Strings.isNullOrEmpty(expected) && expected.equals(actual)) {
			logSuccess("Interaction ID matched", args("fapi_interaction_id", Strings.nullToEmpty(actual)));
			return env;
		} else {
			throw error("Mismatch between interaction IDs", args("expected", Strings.nullToEmpty(expected), "actual", Strings.nullToEmpty(actual)));
		}

	}

}
