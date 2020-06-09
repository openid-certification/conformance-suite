package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class ClientContinuedAfterReceivingIdTokenIssuedInPast extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		throw error("Client has incorrectly called token_endpoint after receiving an id_token with an iat value which is a week in the past from the authorization_endpoint.");
	}

}
