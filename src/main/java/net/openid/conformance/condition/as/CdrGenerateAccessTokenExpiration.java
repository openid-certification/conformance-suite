package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CdrGenerateAccessTokenExpiration extends AbstractCondition {

	@Override
	@PostEnvironment(strings = { "access_token_expiration"})
	public Environment evaluate(Environment env) {
		// CDR access tokens must expire between 2 minutes and 10 minutes after issue
		env.putString("access_token_expiration", "300");
		log("Set access_token_expiration to 300");
		return env;
	}
}
