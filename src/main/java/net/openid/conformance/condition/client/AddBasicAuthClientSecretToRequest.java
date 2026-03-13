package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class AddBasicAuthClientSecretToRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "request_headers", "client" })
	@PostEnvironment(required = "request_headers")
	public Environment evaluate(Environment env) {

		String id = env.getString("client", "client_id");

		if (id == null) {
			throw error("Client ID not found in configuration");
		}

		String secret = env.getString("client", "client_secret");

		if (secret == null) {
			throw error("Client secret not found in configuration");
		}

		JsonObject headers = env.getObject("request_headers");

		String pw = Base64.getEncoder().encodeToString((
			//application/x-www-form-urlencoded as per https://tools.ietf.org/html/rfc6749#section-2.3.1
			URLEncoder.encode(id, StandardCharsets.UTF_8) +
			":" +
			URLEncoder.encode(secret, StandardCharsets.UTF_8)
			).getBytes());

		headers.addProperty("Authorization", "Basic " + pw);

		logSuccess("Added basic authorization header", headers);

		return env;
	}

}
