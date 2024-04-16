package net.openid.conformance.condition.as;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateParEndpointDpopProofNonce extends AbstractValidateDpopProofNonce {

	@Override
	@PreEnvironment(required = {"incoming_dpop_proof"})
//	@PostEnvironment(strings = "par_endpoint_dpop_nonce_error")  // optional variable set
	public Environment evaluate(Environment env) {
		final String ERROR_KEY = "par_endpoint_dpop_nonce_error";
		env.removeNativeValue(ERROR_KEY);
		String expectedNonce = env.getString("authorization_server_dpop_nonce"); // check for server side saved nonce
		if(isValidDpopNonce(env, expectedNonce)) {
			logSuccess("PAR endpoint DPoP nonce matches expected value", args("expected", expectedNonce));
		} else {
			if(null != expectedNonce) {
				// saves expected nonce to be used by CreatePAREndpointDpopErrorResponse to return DPoP nonce error
				env.putString(ERROR_KEY, expectedNonce);
				log("PAR endpoint DPoP nonce is invalid", args("expected", expectedNonce));
			}
		}
		return env;
	}
}
