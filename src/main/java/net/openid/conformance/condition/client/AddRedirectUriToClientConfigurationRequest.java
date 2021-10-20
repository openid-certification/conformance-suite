package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddRedirectUriToClientConfigurationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "registration_client_endpoint_request_body", strings = "redirect_uri")
	@PostEnvironment(required = "registration_client_endpoint_request_body")
	public Environment evaluate(Environment env) {

		JsonObject clientConfigRequest = env.getObject("registration_client_endpoint_request_body");

		String redirectUri = env.getString("redirect_uri");
		if (Strings.isNullOrEmpty(redirectUri)) {
			throw error("No redirect_uri found");
		}

		JsonArray redirectUris = new JsonArray();
		redirectUris.add(redirectUri);
		clientConfigRequest.add("redirect_uris", redirectUris);

		log("Added redirect_uris array to client configuration endpoint request", args("registration_client_endpoint_request", clientConfigRequest));

		return env;
	}
}
