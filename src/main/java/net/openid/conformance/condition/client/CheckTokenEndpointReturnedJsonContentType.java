package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckTokenEndpointReturnedJsonContentType extends AbstractCheckEndpointContentTypeReturned {
	public static String tokenEndpointResponseWasJsonKey = "token_endpoint_response_was_json";

	@Override
	@PreEnvironment(required = "token_endpoint_response_headers")
	public Environment evaluate(Environment env) {

		env.putBoolean(tokenEndpointResponseWasJsonKey, false);

		env = checkContentType(env, "token_endpoint_response_headers", "", "application/json");

		env.putBoolean(tokenEndpointResponseWasJsonKey, true);

		return env;
	}

}
