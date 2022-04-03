package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.common.AbstractInvalidateJwsSignature;
import net.openid.conformance.testmodule.Environment;

public class InvalidateDpopProofSignature extends AbstractInvalidateJwsSignature {

	@Override
	@PreEnvironment(strings = "dpop_proof")
	@PostEnvironment(strings = "dpop_proof")
	public Environment evaluate(Environment env) {
		return invalidateSignature(env, "dpop_proof");
	}

}
