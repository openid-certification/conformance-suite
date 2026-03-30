package net.openid.conformance.vci10issuer.condition.clientattestation;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractCallEndpointWithPost;
import net.openid.conformance.openid.federation.IgnoreErrorsErrorHandler;
import net.openid.conformance.testmodule.Environment;

public class CallClientAttestationChallengeEndpoint extends AbstractCallEndpointWithPost {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(required = "challenge_endpoint_response")
	public Environment evaluate(Environment env) {
		final String endpointUri = env.getString("server", "challenge_endpoint");

		return callEndpointWithPost(env, new IgnoreErrorsErrorHandler(), null, null, endpointUri, "challenge endpoint", "challenge_endpoint_response");
	}
}
