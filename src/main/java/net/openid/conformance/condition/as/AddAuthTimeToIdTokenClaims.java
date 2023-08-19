package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddAuthTimeToIdTokenClaims extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "auth_time")
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("id_token_claims");

		String authTime = env.getString("auth_time");
		claims.addProperty("auth_time", Long.parseLong(authTime));

		env.putObject("id_token_claims", claims);

		logSuccess("Added auth_time to ID token claims", args("id_token_claims", claims, "auth_time", authTime));

		return env;

	}

}
