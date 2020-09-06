package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureParInvalidHTTPMethodError extends AbstractCondition {

	private static final int HTTP_METHOD_NOT_ALLOWED = 405;

	@Override
	public Environment evaluate(Environment env) {

		String key = "pushed_authorization_endpoint_response_http_status";
		Integer status = env.getInteger(key);
		if (status == null) {
			throw error("Integer '"+key + "' not found in environment");
		}

		if (status != HTTP_METHOD_NOT_ALLOWED) {
			throw error("Invalid pushed authorization endpoint response http status code",
				args("expected", HTTP_METHOD_NOT_ALLOWED, "actual", status));
		}

		logSuccess("pushed authorization endpoint returned expected HTTP error", args("actual", status));
		return env;

	}
}
