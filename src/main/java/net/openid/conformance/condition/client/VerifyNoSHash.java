package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class VerifyNoSHash extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token")
	public Environment evaluate(Environment env) {

		String hash = env.getString("id_token", "claims.s_hash");
		if (hash != null) {
			throw error("s_hash has been returned in ID token, when it shouldn't have been");
		}

		logSuccess("ID Token is correctly missing s_hash");

		return env;
	}

}
