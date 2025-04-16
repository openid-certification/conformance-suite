package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractCallEndpointWithGet;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResponseErrorHandler;

import java.util.List;

public abstract class AbstractCallFederationEndpointAndReturnFullResponse extends AbstractCallEndpointWithGet {

	protected abstract List<MediaType> getAcceptHeader();

	@Override
	@PreEnvironment(strings = "federation_endpoint_url")
	@PostEnvironment(required = "federation_endpoint_response")
	public Environment evaluate(Environment env) {
		return callEntityStatementEndpoint(env, new IgnoreErrorsErrorHandler());
	}

	protected Environment callEntityStatementEndpoint(Environment env, ResponseErrorHandler errorHandler) {
		final String endpointUri = env.getString("federation_endpoint_url");
		final String endpointName = "federation endpoint";
		final String envResponseKey = "federation_endpoint_response";
		return callEndpointWithGet(env, errorHandler, getAcceptHeader(), endpointUri, endpointName, envResponseKey);
	}
}
