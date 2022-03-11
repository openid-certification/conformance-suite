package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPIValidateBackchannelRequestObjectSigningAlg extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"backchannel_request_object"})
	public Environment evaluate(Environment env) {

		String alg = env.getString("backchannel_request_object", "header.alg");

		if (alg.equals("PS256") || alg.equals("ES256")) {
			logSuccess("Request object was signed with a permitted algorithm", args("alg", alg));
			return env;
		}

		throw error("Request object must be signed with PS256 or ES256", args("alg", alg));
	}
}
