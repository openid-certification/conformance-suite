package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddCHashToIdTokenClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token_claims", strings = "c_hash")
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("id_token_claims");

		String hash = env.getString("c_hash");

		claims.addProperty("c_hash", hash);

		env.putObject("id_token_claims", claims);

		logSuccess("Added c_hash to ID token claims", args("id_token_claims", claims, "c_hash", hash));

		return env;

	}

}
