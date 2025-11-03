package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class OIDSSFEnsureNoAuthorizationHeaderIsPresentInPushRequest extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject headers = env.getElementFromObject("ssf", "push_request.headers").getAsJsonObject();
		if (headers.has("authorization")) {
			logFailure("Unexpected authorization header found in push delivery request for delivery config without push authorization header", args("headers", headers, "authorization", headers.get("authorization")));
			return env;
		}

		logSuccess("No push Authorization header found in push delivery request for delivery config without push authorization header", args("headers", headers));

		return env;
	}
}
