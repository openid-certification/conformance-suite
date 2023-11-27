package net.openid.conformance.condition.as;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateTokenEndpointDpopErrorResponse extends AbstractCreateDpopErrorResponse {

	@Override
	@PreEnvironment(strings = {"token_endpoint_dpop_nonce_error"})
	@PostEnvironment(required = {"token_endpoint_response", "token_endpoint_response_headers"})
	public Environment evaluate(Environment env) {
		String expectedNonce = env.getString("token_endpoint_dpop_nonce_error");
		return createAuthorizationServerEndpointDpopErrorResponse(env, "token_endpoint_response", expectedNonce);
	}

}
