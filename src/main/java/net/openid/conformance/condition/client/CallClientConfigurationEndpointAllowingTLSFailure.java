package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.web.client.RestClientException;

public class CallClientConfigurationEndpointAllowingTLSFailure extends CallClientConfigurationEndpoint {

	public static final String RESPONSE_SSL_ERROR_KEY = "client_configuration_endpoint_response_ssl_error";

	@Override
	protected boolean allowJsonParseFailure() {
		// as a special case, we allow a html error as TLS failures may be handled by an inflexible TLS terminator
		return true;
	}

	@Override
	@PreEnvironment(required = "client")
	public Environment evaluate(Environment env) {
		env.putBoolean(RESPONSE_SSL_ERROR_KEY, false);
		return super.evaluate(env);
	}

	@Override
	protected Environment handleClientException(Environment env, String registrationClientUri, RestClientException e) {
		if (TlsConnectionFailure.isTlsConnectionFailure(e)) {
			env.putBoolean(RESPONSE_SSL_ERROR_KEY, true);
			logSuccess("Call to client configuration endpoint "+registrationClientUri+" was rejected at the TLS or connection level", ex(e));
			return env;
		}
		return super.handleClientException(env, registrationClientUri, e);
	}

}
