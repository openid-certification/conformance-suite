package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CallDynamicRegistrationEndpointExpectingError extends AbstractCallDynamicRegistrationEndpoint {

	@Override
	@PreEnvironment(required = {"server", "dynamic_registration_request"})
	@PostEnvironment(required = "dynamic_registration_endpoint_response")
	public Environment evaluate(Environment env) {

		return callDynamicRegistrationEndpoint(env);
	}

	@Override
	protected Environment onRegistrationEndpointResponse(Environment env, JsonObject response) {

		env.putObject("dynamic_registration_endpoint_response", response);
		return env;
	}

	@Override
	protected Environment onRegistrationEndpointError(Environment env, Throwable e, int code, String status, String body) {

		logSuccess("Dynamic registration endpoint returned an error", args("code", code, "status", status, "body", body));

		try {
			JsonElement response = new JsonParser().parse(body);
			env.putObject("dynamic_registration_endpoint_response", response.getAsJsonObject());
		} catch (JsonParseException | IllegalStateException parseError) {
			throw error("Response from dynamic registration endpoint does not appear to be a JSON object", parseError);
		}

		return env;
	}
}
