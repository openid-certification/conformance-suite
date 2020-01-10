package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class AuthorizationEndpointRedirectedBackUnexpectedly extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		throw error("Authorization server redirected back in a case where it should not");
	}
}
