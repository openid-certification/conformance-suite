package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPI2ValidateRequestObjectSigningAlg extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_request_object"})
	public Environment evaluate(Environment env) {

		String alg = env.getString("authorization_request_object", "header.alg");

		if (alg.equals("PS256") || alg.equals("ES256") || alg.equals("EdDSA") ) {
			logSuccess("Request object was signed with a permitted algorithm", args("alg", alg));

			return env;
		}

		throw error("Request object must be signed with PS256, ES256, or EdDSA", args("alg", alg));
	}
}
