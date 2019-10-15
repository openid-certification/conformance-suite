package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddInvalidNonceValueToIdToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token_claims")
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("id_token_claims");

		String nonce = env.getString("id_token_claims", "nonce");

		//Add number 1 onto end of nonce string
		String concat = (nonce + 1);

		claims.addProperty("nonce", concat);

		env.putObject("id_token_claims", claims);

		logSuccess("Added invalid nonce to ID token claims", args("id_token_claims", claims, "nonce", concat));

		return env;

	}

}
