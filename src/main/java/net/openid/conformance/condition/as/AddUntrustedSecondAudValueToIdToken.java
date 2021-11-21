package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddUntrustedSecondAudValueToIdToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token_claims")
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {


		String aud = env.getString("id_token_claims", "aud");

		JsonArray audArray = new JsonArray();
		audArray.add(aud);
		audArray.add(aud + 1);

		JsonObject claims = env.getObject("id_token_claims");
		claims.add("aud", audArray);

		logSuccess("Added a second, invalid aud value in ID token claims", args("id_token_claims", claims, "aud", audArray));

		return env;

	}

}
