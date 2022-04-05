package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * PAR-2.2.0 : This class checks for correct HTTP response status code from PAR endpoint
 */
public class CheckPAREndpointResponse201WithNoError extends AbstractCondition {

	private static final int HTTP_STATUS_CREATED = 201;

	@Override
	@PreEnvironment(required = {CallPAREndpoint.RESPONSE_KEY})
	public Environment evaluate(Environment env) {

		//if response code is not 201 then throw error
		Integer status = env.getInteger(CallPAREndpoint.RESPONSE_KEY, "status");

		if (status != HTTP_STATUS_CREATED) {
			throw error("Invalid pushed authorization request endpoint response http status code",
				args("expected", HTTP_STATUS_CREATED, "actual", status));
		}

		JsonObject resp = env.getElementFromObject(CallPAREndpoint.RESPONSE_KEY, "body_json").getAsJsonObject();

		JsonElement errorResponse = resp.get("error");

		if (errorResponse != null) {
			throw error("pushed authorization request endpoint error response."
				, args("actual", resp.toString()));
		}

		logSuccess("pushed authorization request endpoint correct response.");

		return env;


	}

}
