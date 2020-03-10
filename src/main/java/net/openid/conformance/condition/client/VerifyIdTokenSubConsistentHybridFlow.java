package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class VerifyIdTokenSubConsistentHybridFlow extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "authorization_endpoint_id_token", "token_endpoint_id_token" })
	public Environment evaluate(Environment env) {

		String subAuth = env.getString("authorization_endpoint_id_token", "claims.sub");
		String subToken = env.getString("token_endpoint_id_token", "claims.sub");

		if (!subAuth.equals(subToken)) {
			throw error("\"sub\" in authorization endpoint id_token doesn't match with \"sub\" in token endpoint id_token",
				args("sub_auth_endpoint", subAuth, "sub_token_endpoint", subToken));
		}

		logSuccess("authorization endpoint and token endpoint id_token have same sub",
			args("sub_auth_endpoint", subAuth, "sub_token_endpoint", subToken));
		return env;
	}

}
