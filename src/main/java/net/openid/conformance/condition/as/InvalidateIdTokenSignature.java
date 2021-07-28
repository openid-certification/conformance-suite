package net.openid.conformance.condition.as;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.common.AbstractInvalidateJwsSignature;
import net.openid.conformance.testmodule.Environment;

public class InvalidateIdTokenSignature extends AbstractInvalidateJwsSignature {

	@Override
	@PreEnvironment(strings = "id_token")
	@PostEnvironment(strings = "id_token")
	public Environment evaluate(Environment env) {
		return invalidateSignature(env, "id_token");
	}

}
