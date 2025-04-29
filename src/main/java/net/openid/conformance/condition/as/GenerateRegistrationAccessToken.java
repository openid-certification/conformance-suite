package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

public class GenerateRegistrationAccessToken extends AbstractCondition {

	@Override
	@PostEnvironment(strings = { "registration_access_token" })
	public Environment evaluate(Environment env) {

		String accessToken = RandomStringUtils.secure().nextAlphanumeric(50);

		logSuccess("Generated registration access token", args("access_token", accessToken));

		env.putString("registration_access_token", accessToken);

		return env;

	}

}
