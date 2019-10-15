package net.openid.conformance.condition.as;

import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;

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
