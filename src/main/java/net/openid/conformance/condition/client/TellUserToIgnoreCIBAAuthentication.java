package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class TellUserToIgnoreCIBAAuthentication extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		log("Waiting for CIBA authorization to time out. If the AS respects 'requested_expiry' this will happen within 30 seconds.");

		return env;

	}

}
