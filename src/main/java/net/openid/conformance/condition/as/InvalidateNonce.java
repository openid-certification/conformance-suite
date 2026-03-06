package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class InvalidateNonce extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "nonce")
	@PostEnvironment(strings = "nonce")
	public Environment evaluate(Environment env) {

		String originalNonce = env.getString("nonce");
		String invalidNonce = originalNonce + "INVALID";

		env.putString("nonce", invalidNonce);

		log("Replaced nonce with an invalid value",
			args("original_nonce", originalNonce, "invalid_nonce", invalidNonce));

		return env;
	}
}
