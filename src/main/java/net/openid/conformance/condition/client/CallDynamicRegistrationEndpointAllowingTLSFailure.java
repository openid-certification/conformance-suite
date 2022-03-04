package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import javax.net.ssl.SSLException;
import java.net.SocketException;

public class CallDynamicRegistrationEndpointAllowingTLSFailure extends CallDynamicRegistrationEndpoint {

	public static final String RESPONSE_SSL_ERROR_KEY = "dynamic_registration_endpoint_response_ssl_error";

	@Override
	protected boolean allowJsonParseFailure() {
		// as a special case, we allow a html error as TLS failures may be handled by an inflexible TLS terminator
		return true;
	}

	@Override
	@PreEnvironment(required = {"server", "dynamic_registration_request"})
	public Environment evaluate(Environment env) {
		env.putBoolean(RESPONSE_SSL_ERROR_KEY, false);
		return super.evaluate(env);
	}

	@Override
	protected Environment handleClientException(Environment env, RestClientException e) {
		if (e instanceof ResourceAccessException &&
			(e.getCause() instanceof SSLException || e.getCause() instanceof SocketException)) {
			env.putBoolean(RESPONSE_SSL_ERROR_KEY, true);
			logSuccess("Call to dynamic registration endpoint failed due to a TLS issue", ex(e));
			return env;
		}
		return super.handleClientException(env, e);
	}

}
