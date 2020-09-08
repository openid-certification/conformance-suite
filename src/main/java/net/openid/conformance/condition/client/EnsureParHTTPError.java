package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureParHTTPError extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String key = "pushed_authorization_endpoint_response_http_status";
		Integer status = env.getInteger(key);
		if (status == null) {
			throw error("Integer '"+key + "' not found in environment");
		}

		if (status < 400 || status >= 600) {
			throw error("Invalid pushed authorization endpoint response http status code",
				args("expected", "4xx or 5xx", "actual", status));
		}

		logSuccess("pushed authorization endpoint returned a HTTP 4xx or 5xx error as expected", args("actual", status));
		return env;

	}
}
