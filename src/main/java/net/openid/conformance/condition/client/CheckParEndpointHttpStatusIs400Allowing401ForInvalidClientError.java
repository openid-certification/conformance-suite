package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.apache.hc.core5.http.HttpStatus;

public class CheckParEndpointHttpStatusIs400Allowing401ForInvalidClientError extends AbstractCondition {

	@Override
	@PreEnvironment(required = CallPAREndpoint.RESPONSE_KEY)
	public Environment evaluate(Environment env) {
		Integer httpStatus = env.getInteger(CallPAREndpoint.RESPONSE_KEY, "status");

		if (httpStatus == null) {
			throw error("PAR endpoint HTTP status not found in environment");
		}

		JsonElement bodyJson = env.getElementFromObject(CallPAREndpoint.RESPONSE_KEY, "body_json");
		if (bodyJson == null || !bodyJson.isJsonObject()) {
			throw error("PAR endpoint response body is not a JSON object");
		}
		JsonObject body = bodyJson.getAsJsonObject();
		JsonElement errorElement = body.get("error");
		if (errorElement == null) {
			throw error("Expected 'error' field is not present in PAR endpoint response body");
		}
		String error = OIDFJSON.getString(errorElement);

		if ("invalid_client".equals(error)) {
			if (httpStatus != HttpStatus.SC_BAD_REQUEST && httpStatus != HttpStatus.SC_UNAUTHORIZED) {
				throw error("Invalid HTTP status for error 'invalid_client'",
					args("actual", httpStatus, "expected", "400 or 401"));
			}
		} else {
			if (httpStatus != HttpStatus.SC_BAD_REQUEST) {
				throw error("HTTP status must be 400 for PAR endpoint errors other than invalid_client",
					args("actual", httpStatus, "expected", HttpStatus.SC_BAD_REQUEST));
			}
		}

		logSuccess("PAR endpoint HTTP status code was " + httpStatus + " for error '" + error + "'");
		return env;
	}
}
