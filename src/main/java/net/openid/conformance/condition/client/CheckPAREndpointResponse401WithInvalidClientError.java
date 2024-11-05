package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * PAR-2.3 : This class checks for correct HTTP response status code from PAR endpoint
 */
public class CheckPAREndpointResponse401WithInvalidClientError extends AbstractCondition {

	private static final int HTTP_STATUS_UNAUTHORIZED = 401;

	@Override
	@PreEnvironment(required = {CallPAREndpoint.RESPONSE_KEY})
	public Environment evaluate(Environment env) {

		// If response code is not 401 then throw error
		Integer status = env.getInteger(CallPAREndpoint.RESPONSE_KEY, "status");

		if (status != HTTP_STATUS_UNAUTHORIZED) {
			throw error("Invalid pushed authorization request endpoint response http status code",
				args("expected", HTTP_STATUS_UNAUTHORIZED, "actual", status));
		}

		JsonObject resp = env.getElementFromObject(CallPAREndpoint.RESPONSE_KEY, "body_json").getAsJsonObject();
		String errorCode = OIDFJSON.getString(resp.getAsJsonPrimitive("error"));

		if (errorCode == null) {
			throw error("pushed authorization request endpoint response did not contain an error code."
				, args("response", resp.toString()));
		}

		if (! errorCode.equals("invalid_client")) {
			throw error("Invalid pushed authorization request error code.",
				args("expected", "invalid_client", "actual", errorCode));
		}

		logSuccess("pushed authorization request endpoint failed with correct response.");

		return env;
	}
}
