package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.common.AbstractSignClaimsWithNullAlgorithm;
import net.openid.conformance.testmodule.Environment;

public class SerializeRequestObjectWithNullAlgorithm extends AbstractSignClaimsWithNullAlgorithm {

	@Override
	protected String getClaimsNotFoundErrorMsg() {
		return "Couldn't find request object claims";
	}

	@Override
	protected String getSuccessMsg() {
		return "Serialized the request object";
	}

	@Override
	@PreEnvironment(required = "request_object_claims")
	@PostEnvironment(strings = "request_object")
	public Environment evaluate(Environment env) {
		return signWithNullAlgorithm(env, "request_object_claims", "request_object");
	}

}
