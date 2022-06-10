package net.openid.conformance.condition.as;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.common.AbstractInvalidateJwsSignature;
import net.openid.conformance.testmodule.Environment;

public class InvalidateJarmSignature extends AbstractInvalidateJwsSignature {

	@Override
	@PreEnvironment(strings = "jarm_response")
	@PostEnvironment(strings = "jarm_response")
	public Environment evaluate(Environment env) {
		return invalidateSignature(env, "jarm_response");
	}

}
