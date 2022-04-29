package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractRequestUriFromPARResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = CallPAREndpoint.RESPONSE_KEY)
	@PostEnvironment(strings = "request_uri")
	public Environment evaluate(Environment env) {

		String requestUri = env.getString(CallPAREndpoint.RESPONSE_KEY, "body_json.request_uri");
		if (Strings.isNullOrEmpty(requestUri)) {
			throw error("Couldn't find request_uri in " + "pushed authorization endpoint response");
		}

		Long expiresIn = env.getLong(CallPAREndpoint.RESPONSE_KEY, "body_json.expires_in");
		if (expiresIn == null) {
			throw error("Couldn't find expires_in in " + "pushed authorization endpoint response");
		}
		env.putString("request_uri", requestUri);
		env.putLong("expires_in", expiresIn);

		logSuccess("Extracted the request_uri: " + requestUri);

		return env;
	}

}
