package net.openid.conformance.condition.as;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.common.AbstractSignClaimsWithNullAlgorithm;
import net.openid.conformance.testmodule.Environment;

public class SignJarmWithNullAlgorithm extends AbstractSignClaimsWithNullAlgorithm {

	@Override
	protected String getClaimsNotFoundErrorMsg() {
		return "JARM claims not found";
	}

	@Override
	protected String getSuccessMsg() {
		return "Signed the JARM response with null algorithm";
	}

	@Override
	@PreEnvironment(required = "jarm_response_claims" )
	@PostEnvironment(strings = "jarm_response")
	public Environment evaluate(Environment env) {
		return signWithNullAlgorithm(env, "jarm_response_claims", "jarm_response");
	}

}
