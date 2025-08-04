package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractCallEndpointWithPost;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.MediaType;

import java.util.Collections;
import java.util.List;

public class CallCredentialIssuerNonceEndpoint extends AbstractCallEndpointWithPost {

	protected List<MediaType> getAcceptHeader() {
		return Collections.emptyList();
	}

	@Override
	@PreEnvironment(required = "vci")
	@PostEnvironment(required = "nonce_endpoint_response")
	public Environment evaluate(Environment env) {
		final String endpointUri = env.getString("vci", "credential_issuer_metadata.nonce_endpoint");
		final String endpointName = "nonce endpoint";
		final String envResponseKey = "nonce_endpoint_response";

		return callEndpointWithPost(env, new IgnoreErrorsErrorHandler(), null, null, endpointUri, endpointName, envResponseKey);
	}
}
