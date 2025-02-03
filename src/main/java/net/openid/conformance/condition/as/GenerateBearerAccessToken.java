package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

public class GenerateBearerAccessToken extends AbstractCondition {

	@Override
	@PostEnvironment(strings = { "access_token", "token_type" })
	public Environment evaluate(Environment env) {

		String accessToken = RandomStringUtils.secure().nextAlphanumeric(50);

		logSuccess("Generated access token", args("access_token", accessToken));

		env.putString("access_token", accessToken);
		env.putString("token_type", "Bearer");
		return env;

	}

}
