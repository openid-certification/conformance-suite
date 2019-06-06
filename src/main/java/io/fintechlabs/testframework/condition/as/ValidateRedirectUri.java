package io.fintechlabs.testframework.condition.as;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class ValidateRedirectUri extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "client", "token_endpoint_request" })
	public Environment evaluate(Environment env) {

		String expected = env.getString("client", "redirect_uri");
		String actual = env.getString("token_endpoint_request", "params.redirect_uri");

		if (Strings.isNullOrEmpty(expected)) {
			throw error("Couldn't find redirect uri to compare");
		}

		if (expected.equals(actual)) {
			logSuccess("Found redirect uri", args("redirect_uri", actual));
			return env;
		} else {
			throw error("Didn't find matching redirect uri", args("expected", expected, "actual", actual));
		}

	}

}
