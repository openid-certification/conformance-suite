package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilValidateResourceResponseSigningAlg extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"endpoint_response_jwt"})
	public Environment evaluate(Environment env) {

		String alg = env.getString("endpoint_response_jwt", "header.alg");

		if (alg.equals("PS256")) {
			logSuccess("Response was signed with PS256", args("alg", alg));
			return env;
		}

		throw error("Response must be signed with PS256", args("alg", alg));
	}
}
