package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractCallEndpointWithGet;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;

import java.util.List;

public abstract class AbstractCallFederationEndpointAndReturnFullResponse extends AbstractCallEndpointWithGet {

	protected abstract List<MediaType> getAcceptHeader();

	@Override
	@PreEnvironment(strings = "federation_endpoint_url")
	@PostEnvironment(required = "federation_endpoint_response")
	public Environment evaluate(Environment env) {
		return callEntityStatementEndpoint(env, new DefaultResponseErrorHandler() {
			@Override
			public boolean hasError(ClientHttpResponse response) {
				// Treat all http status codes as 'not an error', so spring never throws an exception due to the http
				// status code meaning the rest of our code can handle http status codes how it likes
				return false;
			}
		});
	}

	protected Environment callEntityStatementEndpoint(Environment env, ResponseErrorHandler errorHandler) {
		final String endpointUri = env.getString("federation_endpoint_url");
		final String endpointName = "federation endpoint";
		final String envResponseKey = "federation_endpoint_response";
		return callEndpointWithGet(env, errorHandler, getAcceptHeader(), endpointUri, endpointName, envResponseKey);
	}
}
