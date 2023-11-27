package net.openid.conformance.condition.as;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreatePAREndpointDpopErrorResponse extends AbstractCreateDpopErrorResponse {

	@Override
	@PreEnvironment(strings = {"par_endpoint_dpop_nonce_error"})
	@PostEnvironment(required = {"par_endpoint_response", "par_endpoint_response_headers"})
	public Environment evaluate(Environment env) {
		String expectedNonce = env.getString("par_endpoint_dpop_nonce_error");
		return createAuthorizationServerEndpointDpopErrorResponse(env, "par_endpoint_response", expectedNonce);
	}

}
