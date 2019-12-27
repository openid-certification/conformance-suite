package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureIdTokenSignatureIsRS256 extends AbstractCheckIdTokenSignatureAlgorithm {

	@Override
	@PreEnvironment(required = "id_token")
	public Environment evaluate(Environment env) {

		return checkIdTokenSignatureAlgorithm(env, "RS256");
	}

}
