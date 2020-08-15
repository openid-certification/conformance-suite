package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureRequestObjectWasSignedWithNone extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_request_object"})
	public Environment evaluate(Environment env) {

		String alg = env.getString("authorization_request_object", "header.alg");

		if ("none".equals(alg)) {
			logSuccess("Request object was signed using algorithm 'none'");
			return env;
		}

		throw error("Request object must be signed with algorithm 'none'", args("actual", alg));
	}
}
