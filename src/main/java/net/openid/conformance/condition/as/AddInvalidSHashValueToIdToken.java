package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddInvalidSHashValueToIdToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token_claims", strings = "s_hash")
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("id_token_claims");

		String hash = env.getString("s_hash");

		//Add number 1 onto end of hash string
		String concat = (hash + 1);

		claims.addProperty("s_hash", concat);

		env.putObject("id_token_claims", claims);

		logSuccess("Added invalid s_hash to ID token claims", args("id_token_claims", claims, "s_hash", concat));

		return env;

	}

}
