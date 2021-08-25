package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class TellUserToDoCIBAAuthentication extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		log("Please authenticate and authorize the request");

		return env;

	}

}
