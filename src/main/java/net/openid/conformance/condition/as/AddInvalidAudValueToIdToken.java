package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddInvalidAudValueToIdToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token_claims")
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("id_token_claims");

		String aud = env.getString("id_token_claims", "aud");

		//Add number 1 onto end of aud string
		String concat = (aud + 1);

		claims.addProperty("aud", concat);

		env.putObject("id_token_claims", claims);

		logSuccess("Added invalid aud to ID token claims", args("id_token_claims", claims, "aud", concat));

		return env;

	}

}
