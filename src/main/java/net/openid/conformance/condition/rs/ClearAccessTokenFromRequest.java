package net.openid.conformance.condition.rs;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class ClearAccessTokenFromRequest extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		env.removeNativeValue("incoming_access_token");

		log("Removed incoming access token from environment");

		return env;

	}

}
