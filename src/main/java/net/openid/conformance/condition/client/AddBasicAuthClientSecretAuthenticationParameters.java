package net.openid.conformance.condition.client;

import java.util.Base64;

import com.google.gson.JsonObject;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddBasicAuthClientSecretAuthenticationParameters extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client")
	@PostEnvironment(required = "token_endpoint_request_headers")
	public Environment evaluate(Environment env) {

		String id = env.getString("client", "client_id");

		if (id == null) {
			throw error("Client ID not found in configuration");
		}

		String secret = env.getString("client", "client_secret");

		if (secret == null) {
			throw error("Client secret not found in configuration");
		}

		JsonObject headers = env.getObject("token_endpoint_request_headers");

		if (headers == null) {
			headers = new JsonObject();
			env.putObject("token_endpoint_request_headers", headers);
		}

		String pw = Base64.getEncoder().encodeToString((id + ":" + secret).getBytes());

		headers.addProperty("Authorization", "Basic " + pw);

		logSuccess("Added basic authorization header", headers);

		return env;
	}

}
