package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Base64;

public class CreateWalletNonce extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "wallet_nonce")
	public Environment evaluate(Environment env) {

		// Generate 32 random bytes and base64url-encode them
		byte[] randomBytes = new byte[32];
		new java.security.SecureRandom().nextBytes(randomBytes);
		String walletNonce = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

		env.putString("wallet_nonce", walletNonce);

		logSuccess("Created wallet_nonce", args("wallet_nonce", walletNonce));

		return env;
	}

}
