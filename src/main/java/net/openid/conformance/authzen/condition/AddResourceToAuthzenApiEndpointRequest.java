package net.openid.conformance.authzen.condition;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddResourceToAuthzenApiEndpointRequest extends AddParameterToAuthzenApiEndpointRequest {

	public AddResourceToAuthzenApiEndpointRequest() {
		super("resource", "authzen_api_endpoint_request_resource");
	}

	@Override
	@PreEnvironment(required = {"authzen_api_endpoint_request", "authzen_api_endpoint_request_resource"})
	@PostEnvironment(required = "authzen_api_endpoint_request")
	public Environment evaluate(Environment env) {
		return addParameterToAuthzenApiEndpointRequest(env);
	}
}
