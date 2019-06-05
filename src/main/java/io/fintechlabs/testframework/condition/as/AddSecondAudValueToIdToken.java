package io.fintechlabs.testframework.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddSecondAudValueToIdToken extends AbstractCondition {

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
