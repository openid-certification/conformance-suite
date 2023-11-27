package net.openid.conformance.condition.as;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateDpopProofResourceRequest extends AbstractValidateDpopProof {

	@Override
	@PreEnvironment(required = {"incoming_dpop_proof", "incoming_request"})
	public Environment evaluate(Environment env) {
		return validateDpopProof(env, true);
	}
}
