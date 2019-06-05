package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExpectExpiredTokenErrorFromTokenEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment( required = "token_endpoint_response" )
	public Environment evaluate(Environment env) {
		String error = env.getString("token_endpoint_response", "error");

		if (Strings.isNullOrEmpty(error)) {
			throw error("error missing from token_endpoint_response.");
		}

		String expected = "expired_token";

		if (!error.equals(expected)) {
			throw error("error value is incorrect",
				args("expected", expected, "actual", error));
		}

		logSuccess("error parameter is correctly '"+error+"'");

		return env;
	}
}
