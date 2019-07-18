package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class CreateRefreshTokenRequest extends AbstractCondition {
	@Override
	@PreEnvironment(strings = {"refresh_token"})
	@PostEnvironment(required = "token_endpoint_request_form_parameters")
	public Environment evaluate(Environment env) {
		JsonObject refreshTokenRequest = new JsonObject();
		refreshTokenRequest.addProperty("grant_type", "refresh_token");
		refreshTokenRequest.addProperty("refresh_token", env.getString("refresh_token"));

		env.putObject("token_endpoint_request_form_parameters", refreshTokenRequest);
		logSuccess("Created token endpoint request parameters", refreshTokenRequest);
		return env;
	}
}
