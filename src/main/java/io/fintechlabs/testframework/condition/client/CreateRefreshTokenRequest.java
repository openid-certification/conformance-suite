package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class CreateRefreshTokenRequest extends AbstractCondition {
	@Override
	@PreEnvironment(strings = {"refresh_token", "redirect_uri", "scope"})
	@PostEnvironment(required = "token_endpoint_request_form_parameters")
	public Environment evaluate(Environment env) {
		JsonObject refreshTokenRequest = new JsonObject();
		refreshTokenRequest.addProperty("grant_type", "refresh_token");
		refreshTokenRequest.addProperty("refresh_token", env.getString("refresh_token"));
		refreshTokenRequest.addProperty("redirect_uri", env.getString("redirect_uri"));
		refreshTokenRequest.addProperty("scope", env.getString("scope"));

		env.putObject("token_endpoint_request_form_parameters", refreshTokenRequest);

		return env;
	}
}
