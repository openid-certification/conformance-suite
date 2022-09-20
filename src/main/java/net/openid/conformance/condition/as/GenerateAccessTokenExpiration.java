package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class GenerateAccessTokenExpiration extends AbstractCondition {

	@Override
	@PostEnvironment(strings = { "access_token_expiration"})
	public Environment evaluate(Environment env) {
		env.putString("access_token_expiration", "900");
		log("Set access_token_expiration to 900");
		return env;
	}

}
