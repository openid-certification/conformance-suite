package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateIdTokenNonce extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token")
	public Environment evaluate(Environment env) {

		String incomingNonce = env.getString("id_token","claims.nonce");

		if (!env.getString("nonce").equals(incomingNonce)) {
			throw error("Nonce values mismatch", args("actual", incomingNonce, "expected", env.getString("nonce")));
		} else {
			logSuccess("Nonce values match", args("nonce", incomingNonce));
		}

		return env;
	}

}
