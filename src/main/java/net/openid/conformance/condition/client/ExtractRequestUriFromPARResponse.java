package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractRequestUriFromPARResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "pushed_authorization_endpoint_response")
	@PostEnvironment(strings = "request_uri")
	public Environment evaluate(Environment env) {

		return extractRequestUri(env, "pushed_authorization_endpoint_response");
	}

	private Environment extractRequestUri(Environment env, String source) {
		String requestUri = env.getString(source, "request_uri");
		if (Strings.isNullOrEmpty(requestUri)) {
			throw error("Couldn't find request_uri in " + source);
		}

		Long expiresIn = env.getLong(source, "expires_in");
		if (expiresIn == null) {
			throw error("Couldn't find expires_in in " + source);
		}
		env.putString("request_uri", requestUri);
		env.putLong("expires_in", expiresIn);

		logSuccess("Extracted the request_uri: " + requestUri);

		return env;
	}
}
