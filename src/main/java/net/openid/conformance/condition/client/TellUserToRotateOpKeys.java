package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class TellUserToRotateOpKeys extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		log("Please rotate the keys on the authorization server then press the 'Start' button.");

		return env;

	}

}
