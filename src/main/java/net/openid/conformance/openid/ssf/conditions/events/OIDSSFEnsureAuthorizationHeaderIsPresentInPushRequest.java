package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class OIDSSFEnsureAuthorizationHeaderIsPresentInPushRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "ssf")
	public Environment evaluate(Environment env) {

		JsonObject headers = env.getElementFromObject("ssf", "push_request.headers").getAsJsonObject();
		if (!headers.has("authorization")) {
			throw error("Push authorization header missing in push delivery request", args("headers", headers));
		}

		String authorizationHeader = env.getString("ssf", "push_request.headers.authorization");
		String expectedAuthorizationHeader = env.getString("ssf", "transmitter.push_endpoint_authorization_header");
		if (!authorizationHeader.equals(expectedAuthorizationHeader)) {
			throw error("Found unexpected authorization header in push delivery request",
				args("expected_authorization_header", expectedAuthorizationHeader, "authorization", authorizationHeader));
		}

		logSuccess("Found expected push authorization header in push delivery request", args("authorization", authorizationHeader));

		return env;
	}
}
