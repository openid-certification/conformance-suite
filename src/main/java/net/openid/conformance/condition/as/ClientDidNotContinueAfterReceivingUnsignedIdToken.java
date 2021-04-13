package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class ClientDidNotContinueAfterReceivingUnsignedIdToken extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		throw error("Client did not send a userinfo request after receiving an unsigned id_token.");
	}

}
