package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class ServerAllowedExpiredAuthorizationCode extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		throw error("Server has incorrectly allowed the use of an expired authorization code.");

	}

}
