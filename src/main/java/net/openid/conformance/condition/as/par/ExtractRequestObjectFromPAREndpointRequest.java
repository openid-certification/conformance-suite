package net.openid.conformance.condition.as.par;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.AbstractExtractRequestObject;
import net.openid.conformance.testmodule.Environment;

public class ExtractRequestObjectFromPAREndpointRequest extends AbstractExtractRequestObject
{

	@Override
	@PreEnvironment(required = {"par_endpoint_http_request", "client", "server_jwks"})
	@PostEnvironment(required = "authorization_request_object")
	public Environment evaluate(Environment env) {
		String requestObjectString = env.getString("par_endpoint_http_request", "body_form_params.request");
		processRequestObjectString(requestObjectString, env);

		return env;
	}

}
