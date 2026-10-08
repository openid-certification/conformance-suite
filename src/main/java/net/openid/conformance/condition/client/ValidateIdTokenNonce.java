package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateIdTokenNonce extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token")
	public Environment evaluate(Environment env) {

		String incomingNonce = env.getString("id_token","claims.nonce");

		String expectedNonce = env.getString("nonce");

		if (expectedNonce == null) {
			if (incomingNonce != null) {
				throw error("id_token contains a nonce claim, but no nonce was sent in the authorization request.",
					args("actual", incomingNonce));
			}
			logSuccess("nonce is not in id_token, as expected.");
			return env;
		}

		if (!expectedNonce.equals(incomingNonce)) {
			throw error("Nonce values mismatch", args("actual", incomingNonce, "expected", expectedNonce));
		} else {
			logSuccess("Nonce values match", args("nonce", incomingNonce));
		}

		return env;
	}

}
