package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractEnsureHttpAuthorizeChallenge extends AbstractCondition {
	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {

		JsonElement headers = env.getElementFromObject("endpoint_response", "headers");
		String endpointName = env.getString("endpoint_response", "endpoint_name");


		JsonElement wwwAuthenticateElement = headers.getAsJsonObject().get("WWW-Authenticate");

		if (wwwAuthenticateElement == null){
			throw error(endpointName + " endpoint has not returned a WWW-Authenticate header");
		}

		String wwwAuthenticate = wwwAuthenticateElement.getAsString().trim();
		String expectedChallenge = getExpectedChallenge();
		if (wwwAuthenticate.startsWith(expectedChallenge)) {
			throw error(endpointName + " endpoint has not returned a WWW-Authenticate header",
				args("challenge", wwwAuthenticate, "expected_challenge", expectedChallenge));
		}

		logSuccess(endpointName + " endpoint returned the expected challenge",
			args(
				"challenge", wwwAuthenticate,
				"expected_challenge", expectedChallenge));
		return env;

	}

	protected abstract String getExpectedChallenge();
}
