package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddInvalidSubValueToIdToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token_claims")
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("id_token_claims");

		String sub = env.getString("id_token_claims", "sub");

		//Add "invalid" onto end of sub string
		String concat = (sub + "invalid");

		claims.addProperty("sub", concat);

		env.putObject("id_token_claims", claims);

		log("Added invalid sub to ID token claims", args("id_token_claims", claims, "sub", concat));

		return env;

	}

}
