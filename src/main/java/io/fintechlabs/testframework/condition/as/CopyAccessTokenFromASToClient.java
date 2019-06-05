package io.fintechlabs.testframework.condition.as;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class CopyAccessTokenFromASToClient extends AbstractCondition {

	@Override
	@PreEnvironment(strings = { "access_token", "token_type" })
	@PostEnvironment(required = "access_token")
	public Environment evaluate(Environment env) {

		String accessTokenString = env.getString("access_token");
		String tokenType = env.getString("token_type");

		JsonObject o = new JsonObject();
		o.addProperty("value", accessTokenString);
		o.addProperty("type", tokenType);

		env.putObject("access_token", o);

		logSuccess("Copied the access token", o);

		return env;



	}

}
