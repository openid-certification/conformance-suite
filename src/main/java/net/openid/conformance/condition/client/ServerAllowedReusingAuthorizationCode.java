package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class ServerAllowedReusingAuthorizationCode extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		throw error("Server has incorrectly allowed a second use of an authorization code; an authorization code is expected to be single use.");

	}

}
