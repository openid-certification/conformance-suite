package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.common.AbstractInvalidateJwsSignature;
import net.openid.conformance.testmodule.Environment;

public class InvalidateRequestObjectSignature extends AbstractInvalidateJwsSignature {

	@Override
	@PreEnvironment(strings = "request_object")
	@PostEnvironment(strings = "request_object")
	public Environment evaluate(Environment env) {
		return invalidateSignature(env, "request_object");
	}

}
