package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class ServerAllowedReusingAuthorisationCode extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		throw error("Server has incorrectly allowed reusing authorisation code while it shouldn't.");

	}

}
