package net.openid.conformance.condition.as;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractRequestObject extends AbstractExtractRequestObject {

	@Override
	@PreEnvironment(required = {"authorization_endpoint_http_request_params", "client", "server_jwks"})
	@PostEnvironment(required = "authorization_request_object")
	public Environment evaluate(Environment env) {
		String requestObjectString = env.getString("authorization_endpoint_http_request_params", "request");
		processRequestObjectString(requestObjectString, env);
		return env;
	}

}
