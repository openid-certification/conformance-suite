package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureContentTypeJson extends AbstractCheckEndpointContentTypeReturned {

	public static String endpointResponseWasJsonKey = "endpoint_response_was_json";

	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {

		env.putBoolean(endpointResponseWasJsonKey, false);

		env = checkContentType(env, "endpoint_response", "headers.", "application/json");

		env.putBoolean(endpointResponseWasJsonKey, true);

		return env;
	}

}
