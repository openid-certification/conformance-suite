package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class TellUserToRejectCIBAAuthentication extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		log("Please reject/cancel the authentication request");

		return env;

	}

}
