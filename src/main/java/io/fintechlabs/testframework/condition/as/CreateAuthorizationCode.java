package io.fintechlabs.testframework.condition.as;

import org.apache.commons.lang3.RandomStringUtils;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class CreateAuthorizationCode extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "authorization_code")
	public Environment evaluate(Environment env) {

		String code = RandomStringUtils.randomAlphanumeric(10);

		env.putString("authorization_code", code);

		logSuccess("Created authorization code", args("authorization_code", code));

		return env;

	}

}
