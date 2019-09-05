package io.fintechlabs.testframework.condition.client;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

import java.util.List;

public class ValidateErrorResponseFromAuthorizationEndpoint extends AbstractCondition {

	private static final List<String> EXPECTED_PARAMS = ImmutableList.of("error", "error_description", "error_uri", "state", "session_state");

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {

		JsonObject callbackParams = env.getObject("authorization_endpoint_response");

		// https://openid.net/specs/openid-connect-core-1_0.html#AuthError
		if (callbackParams.has("error")) {

			JsonObject unexpectedParams = new JsonObject();

			callbackParams.entrySet().forEach(entry -> {
				if (!EXPECTED_PARAMS.contains(entry.getKey())) {
					unexpectedParams.add(entry.getKey(), entry.getValue());
				}
			});

			if (unexpectedParams.size() == 0) {
				logSuccess("error response includes only expected parameters", callbackParams);
			} else {
				throw error("error response includes unexpected parameters", unexpectedParams);
			}
		} else {
			throw error("Authorization server was expected to return an error but did not", callbackParams);
		}
		return env;
	}

}
