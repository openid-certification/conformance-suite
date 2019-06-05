package io.fintechlabs.testframework.condition.as;

import org.apache.commons.lang3.RandomStringUtils;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class GenerateBearerAccessToken extends AbstractCondition {

	@Override
	@PostEnvironment(strings = { "access_token", "token_type" })
	public Environment evaluate(Environment env) {

		String accessToken = RandomStringUtils.randomAlphanumeric(50);

		logSuccess("Generated access token", args("access_token", accessToken));

		env.putString("access_token", accessToken);
		env.putString("token_type", "Bearer");
		return env;

	}

}
