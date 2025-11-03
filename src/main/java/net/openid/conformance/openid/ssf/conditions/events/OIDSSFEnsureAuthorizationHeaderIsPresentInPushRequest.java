package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class OIDSSFEnsureAuthorizationHeaderIsPresentInPushRequest extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject headers = env.getElementFromObject("ssf", "push_request.headers").getAsJsonObject();
		if (!headers.has("authorization")) {
			logFailure("Push authorization header missing in push delivery request", args("headers", headers));
			return env;
		}

		String authorizationHeader = env.getString("ssf", "push_request.headers.authorization");
		String expectedAuthorizationHeader = env.getString("ssf", "transmitter.push_endpoint_authorization_header");
		if (!authorizationHeader.equals(expectedAuthorizationHeader)) {
			logFailure("Found unexpected authorization header found in push delivery request for delivery config push authorization header", args("expected_authorization_header", expectedAuthorizationHeader, "authorization", authorizationHeader));
			return env;
		}

		logSuccess("Found expected push authorization header found in push delivery request", args("authorization", authorizationHeader));

		return env;
	}
}
