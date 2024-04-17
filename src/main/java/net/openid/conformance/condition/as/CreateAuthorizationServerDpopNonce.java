package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.util.RFC6749AppendixASyntaxUtils;
import net.openid.conformance.testmodule.Environment;

public class CreateAuthorizationServerDpopNonce extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "authorization_server_dpop_nonce")
	public Environment evaluate(Environment env) {
		// DPOP spec does not define max length for nonce value
		String nonce = RFC6749AppendixASyntaxUtils.generateNQChar(50, 10, 30);
		env.putString("authorization_server_dpop_nonce", nonce);
		logSuccess("Created Authorization Server DPoP nonce", args("authorization_server_dpop_nonce", nonce));
		return env;
	}
}
