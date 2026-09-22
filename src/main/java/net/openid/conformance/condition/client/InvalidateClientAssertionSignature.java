package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.common.AbstractInvalidateJwsSignature;
import net.openid.conformance.testmodule.Environment;

public class InvalidateClientAssertionSignature extends AbstractInvalidateJwsSignature {

	@Override
	@PreEnvironment(strings = "client_assertion")
	@PostEnvironment(strings = "client_assertion")
	public Environment evaluate(Environment env) {
		return invalidateSignature(env, "client_assertion");
	}

}
