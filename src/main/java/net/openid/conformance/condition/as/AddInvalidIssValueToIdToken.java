package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddInvalidIssValueToIdToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token_claims")
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("id_token_claims");

		String iss = env.getString("id_token_claims", "iss");

		//Add number 1 onto end of iss string
		String concat = (iss + 1);

		claims.addProperty("iss", concat);

		env.putObject("id_token_claims", claims);

		logSuccess("Added invalid iss to ID token claims", args("id_token_claims", claims, "iss", concat));

		return env;

	}

}
