package net.openid.conformance.condition.as;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.common.AbstractInvalidateJwsSignature;
import net.openid.conformance.testmodule.Environment;

public class InvalidateSdJwtKbSignature extends AbstractInvalidateJwsSignature {

	@Override
	@PreEnvironment(strings = "credential")
	@PostEnvironment(strings = "credential")
	public Environment evaluate(Environment env) {
		String credential = env.getString("credential");
		int lastTilde = credential.lastIndexOf('~');
		if (lastTilde < 0 || lastTilde == credential.length() - 1) {
			throw error("SD-JWT credential does not contain a key binding JWT",
				args("credential", credential));
		}
		String prefix = credential.substring(0, lastTilde + 1);
		String kbJwt = credential.substring(lastTilde + 1);

		String invalidKbJwt = invalidateSignatureString("credential", kbJwt);

		env.putString("credential", prefix + invalidKbJwt);

		log("Invalidated KB-JWT signature in SD-JWT credential");

		return env;
	}
}
