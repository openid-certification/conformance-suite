package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.CreateAuthorizationEndpointResponseParams;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;

public class CallDirectPostEndpoint extends AbstractCallOAuthEndpoint {

	@Override
	@PreEnvironment(required = { "authorization_request_object", CreateAuthorizationEndpointResponseParams.ENV_KEY }) // FIXME correct
	@PostEnvironment(required = "direct_post_response")
	public Environment evaluate(Environment env) {

		return callDirectPostEndpoint(env, new DefaultResponseErrorHandler() {
			@Override
			public boolean hasError(ClientHttpResponse response) throws IOException {
				// Treat all http status codes as 'not an error', so spring never throws an exception due to the http
				// status code meaning the rest of our code can handle http status codes how it likes
				return false;
			}
		});
	}


	public Environment callDirectPostEndpoint(Environment env, ResponseErrorHandler errorHandler) {

		final String requestFormParametersEnvKey = "direct_post_request_form_parameters";
		final String requestHeadersEnvKey = null;
		final String tokenEndpointUri = env.getString("authorization_request_object", "claims.response_uri");
		final String endpointName = "response uri endpoint";
		final String envResponseKey = "direct_post_response";

		return callOAuthEndpoint(env, errorHandler, requestFormParametersEnvKey, requestHeadersEnvKey, tokenEndpointUri, endpointName, envResponseKey);

	}

}
