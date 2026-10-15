package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class AddBasicAuthClientSecretAuthenticationParametersToAuthzenApiRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client")
	@PostEnvironment(required = "authzen_api_endpoint_request_headers")
	public Environment evaluate(Environment env) {

		// An empty value would still produce a Basic header, which the PDP would reject for a
		// request the test meant to authenticate.
		String id = getRequiredCredential(env, "client_id");
		String secret = getRequiredCredential(env, "client_secret");

		JsonObject headers = env.getObject("authzen_api_endpoint_request_headers");

		if (headers == null) {
			headers = new JsonObject();
			env.putObject("authzen_api_endpoint_request_headers", headers);
		}

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

	private String getRequiredCredential(Environment env, String field) {
		String value = env.getString("client", field);
		if (value == null) {
			throw error("'" + field + "' field is missing from the 'Client' section in the test configuration");
		}
		if (value.isBlank()) {
			throw error("'" + field + "' field in the 'Client' section of the test configuration is empty");
		}
		return value;
	}

}
