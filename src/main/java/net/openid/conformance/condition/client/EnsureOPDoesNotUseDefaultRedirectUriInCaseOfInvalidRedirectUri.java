package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class EnsureOPDoesNotUseDefaultRedirectUriInCaseOfInvalidRedirectUri extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		throw error("An invalid redirect_uri was included in the authorization request but the OP redirected to a " +
			"default redirect_uri instead of displaying an error page");
	}

}
