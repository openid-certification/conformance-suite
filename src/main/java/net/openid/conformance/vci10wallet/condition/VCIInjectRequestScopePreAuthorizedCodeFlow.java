package net.openid.conformance.vci10wallet.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class VCIInjectRequestScopePreAuthorizedCodeFlow extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String credentialScopeAlias = "eudi.pid.1";
		env.putString("effective_authorization_endpoint_request", "scope", credentialScopeAlias);

		log("Injected scope", args("scope", credentialScopeAlias));

		return env;
	}
}
