package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddAudValueAsArrayToIdToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token_claims")
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {


		String aud = env.getString("id_token_claims", "aud");

		JsonArray audArray = new JsonArray();
		audArray.add(aud);

		JsonObject claims = env.getObject("id_token_claims");
		claims.add("aud", audArray);

		logSuccess("Added the aud value as an array to ID token claims", args("id_token_claims", claims, "aud", audArray));

		return env;

	}

}
