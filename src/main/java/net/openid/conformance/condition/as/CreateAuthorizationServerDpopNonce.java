package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

public class CreateAuthorizationServerDpopNonce extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "authorization_server_dpop_nonce")
	public Environment evaluate(Environment env) {
		String nonce = RandomStringUtils.randomAlphanumeric(20);
		env.putString("authorization_server_dpop_nonce", nonce);
		logSuccess("Created Authorization Server DPoP nonce", args("authorization_server_dpop_nonce", nonce));
		return env;
	}
}
