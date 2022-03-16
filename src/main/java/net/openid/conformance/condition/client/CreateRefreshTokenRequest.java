package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateRefreshTokenRequest extends AbstractCondition {
	@Override
	@PreEnvironment(strings = {"refresh_token"})
	@PostEnvironment(required = "token_endpoint_request_form_parameters")
	public Environment evaluate(Environment env) {
		JsonObject refreshTokenRequest = new JsonObject();
		refreshTokenRequest.addProperty("grant_type", "refresh_token");
		refreshTokenRequest.addProperty("refresh_token", env.getString("refresh_token"));

		env.putObject("token_endpoint_request_form_parameters", refreshTokenRequest);

		// Remove headers as well, so that we're truly starting a 'new' request
		env.removeObject("token_endpoint_request_headers");

		logSuccess("Created token endpoint request parameters", refreshTokenRequest);
		return env;
	}
}
