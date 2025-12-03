package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractCallEndpointWithGet;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.MediaType;

import java.util.List;

public class CallJwksUriEndpointWithGetAndReturnFullResponse extends AbstractCallEndpointWithGet {

	protected List<MediaType> getAcceptHeader() {
		return List.of(MediaType.APPLICATION_JSON);
	}

	@Override
	@PreEnvironment(strings = { "jwks_uri" })
	@PostEnvironment(required = "jwks_uri_response")
	public Environment evaluate(Environment env) {
		final String endpointUri = env.getString("jwks_uri");
		final String endpointName = "jwks_uri";
		final String envResponseKey = "jwks_uri_response";

		return callEndpointWithGet(env, new IgnoreErrorsErrorHandler(), getAcceptHeader(), endpointUri, endpointName, envResponseKey);
	}
}
