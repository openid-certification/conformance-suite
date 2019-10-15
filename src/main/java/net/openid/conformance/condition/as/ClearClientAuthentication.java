package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class ClearClientAuthentication extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		// remove the client auth from the environment
		env.removeNativeValue("client_authentication_success");
		env.removeObject("client_authentication");

		return env;

	}

}
