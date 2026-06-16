package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddUnexpectedAcrToIdToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token_claims")
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("id_token_claims");

		String acr = "urn:connectid:unexpected_acr";
		claims.addProperty("acr", acr);

		env.putObject("id_token_claims", claims);

		logSuccess("Added unexpected acr value to ID token claims", args("id_token_claims", claims, "acr", acr));

		return env;
	}

}
