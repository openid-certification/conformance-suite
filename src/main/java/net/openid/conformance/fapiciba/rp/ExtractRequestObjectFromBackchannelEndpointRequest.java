package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.AbstractExtractRequestObject;
import net.openid.conformance.testmodule.Environment;

public class ExtractRequestObjectFromBackchannelEndpointRequest extends AbstractBackchannelExtractRequestObject
{
	@Override
	@PreEnvironment(required = {"backchannel_endpoint_http_request", "client", "server_jwks"})
	@PostEnvironment(required = "backchannel_request_object")
	public Environment evaluate(Environment env) {
		String requestObjectString = env.getString("backchannel_endpoint_http_request", "body_form_params.request");
		processRequestObjectString(requestObjectString, env);

		return env;
	}
}
