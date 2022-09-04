package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class RemoveAccessTokenExpiration extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		env.removeNativeValue("access_token_expiration");
		log("Removed access_token_expiration");
		return env;
	}

}
