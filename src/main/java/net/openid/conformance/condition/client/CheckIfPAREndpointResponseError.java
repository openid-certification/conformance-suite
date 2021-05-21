package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * PAR-2.2.0 : This class checks for correct HTTP response status code from PAR endpoint
 */
public class CheckIfPAREndpointResponseError extends AbstractCondition {

	private static final int HTTP_STATUS_CREATED = 201;

	@Override
	@PreEnvironment(required = {"pushed_authorization_endpoint_response",
		"pushed_authorization_endpoint_response_headers"})
	public Environment evaluate(Environment env) {

		//if response code is not 201 then throw error
		Integer status = env.getInteger("pushed_authorization_endpoint_response_http_status");

		if (status != HTTP_STATUS_CREATED) {
			throw error("Invalid pushed authorization request endpoint response http status code",
				args("expected", HTTP_STATUS_CREATED, "actual", status));
		}

		JsonObject resp = env.getObject("pushed_authorization_endpoint_response").getAsJsonObject();

		JsonElement errorResponse = resp.get("error");

		if (errorResponse != null) {
			throw error("pushed authorization request endpoint error response."
				, args("actual", resp.toString()));
		}

		logSuccess("pushed authorization request endpoint correct response.");

		return env;


	}

}
