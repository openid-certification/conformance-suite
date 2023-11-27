package net.openid.conformance.condition.rs;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.AbstractCreateDpopErrorResponse;
import net.openid.conformance.testmodule.Environment;

public class CreateResourceEndpointDpopErrorResponse extends AbstractCreateDpopErrorResponse {

	@Override
	@PreEnvironment(strings = {"resource_endpoint_dpop_nonce_error"})
	@PostEnvironment(required = {"resource_endpoint_response_headers"})
	public Environment evaluate(Environment env) {
		String expectedNonce = env.getString("resource_endpoint_dpop_nonce_error");
		return createResourceServerEndpointDpopErrorResponse(env, "resource_endpoint_response", expectedNonce);
	}

}
