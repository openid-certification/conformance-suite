package net.openid.conformance.condition.rs;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.util.RFC6749AppendixASyntaxUtils;
import net.openid.conformance.testmodule.Environment;

public class CreateResourceServerDpopNonce extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "resource_server_dpop_nonce")
	public Environment evaluate(Environment env) {
		String nonce = RFC6749AppendixASyntaxUtils.generateNQChar(50, 10, 30);
		env.putString("resource_server_dpop_nonce", nonce);
		logSuccess("Created Resource Server nonce", args("resource_server_dpop_nonce", nonce));
		return env;
	}
}
