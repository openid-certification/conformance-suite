package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

import java.util.List;

public class EnsureErrorTokenEndpointSlowdownOrAuthorizationPending extends AbstractCondition {

	@Override
	@PreEnvironment( required = "token_endpoint_response" )
	public Environment evaluate(Environment env) {
		String tokenEndpointError = env.getString("token_endpoint_response", "error");

		if (Strings.isNullOrEmpty(tokenEndpointError)) {
			throw error("error missing from token_endpoint_response.");
		}

		List<String> lookFor = ImmutableList.of("slow_down", "authorization_pending");

		if (!lookFor.contains(tokenEndpointError)) {
			throw error("error not met expected as 'slow_down' or 'authorization_pending'", args("expected", lookFor, "actual", tokenEndpointError));
		}

		logSuccess("error met 'slow_down' or 'authorization_pending'", args("error", tokenEndpointError));

		return env;
	}
}
