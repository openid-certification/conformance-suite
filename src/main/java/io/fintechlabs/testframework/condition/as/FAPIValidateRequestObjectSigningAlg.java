package io.fintechlabs.testframework.condition.as;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class FAPIValidateRequestObjectSigningAlg extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_request_object"})
	public Environment evaluate(Environment env) {

		String alg = env.getString("authorization_request_object", "header.alg");

		if (alg.equals("PS256") || alg.equals("ES256")) {
			logSuccess("Request object was signed with a permitted algorithm", args("alg", alg));
			return env;
		}

		throw error("Request object should be signed with PS256 or ES256", args("alg", alg));
	}
}
