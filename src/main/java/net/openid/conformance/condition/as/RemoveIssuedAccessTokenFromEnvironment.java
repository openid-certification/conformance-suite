package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class RemoveIssuedAccessTokenFromEnvironment extends AbstractCondition {

	@Override
	@PreEnvironment(strings = { "access_token", "token_type" })
	public Environment evaluate(Environment env) {

		env.removeNativeValue("access_token");
		env.removeNativeValue("token_type");

		log("Removed access_token and token_type from environment");
		return env;

	}

}
