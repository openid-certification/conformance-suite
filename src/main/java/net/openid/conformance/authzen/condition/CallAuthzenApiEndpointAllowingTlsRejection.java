package net.openid.conformance.authzen.condition;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.TlsConnectionFailure;
import net.openid.conformance.testmodule.Environment;
import org.springframework.web.client.RestClientException;

/**
 * Calls the AuthZEN API endpoint without a client certificate the PDP can accept, for the mtls
 * 401 modules. A PDP that authenticates PEPs by mTLS may refuse such a client in the TLS
 * handshake, before any HTTP response exists; that refusal satisfies Section 11.3 as a 401
 * would. It sets {@code authzen_api_endpoint_response_tls_rejected} and leaves no
 * {@code authzen_api_endpoint_response}, so the caller checks the status only of an HTTP answer.
 */
public class CallAuthzenApiEndpointAllowingTlsRejection extends CallAuthzenApiEndpointAllowingJsonParseFailure {

	public static final String TLS_REJECTED_KEY = "authzen_api_endpoint_response_tls_rejected";

	@Override
	@PreEnvironment(required = {"authzen_api_endpoint_request"}, strings = "authzen_api_endpoint")
	public Environment evaluate(Environment env) {
		env.putBoolean(TLS_REJECTED_KEY, false);
		env.removeObject("authzen_api_endpoint_response");
		return super.evaluate(env);
	}

	@Override
	Environment handleClientException(Environment env, RestClientException e) {
		if (TlsConnectionFailure.isTlsConnectionFailure(e)) {
			env.putBoolean(TLS_REJECTED_KEY, true);
			logSuccess("The PDP refused the connection at the TLS level, which rejects the unauthenticated request", ex(e));
			return env;
		}
		return super.handleClientException(env, e);
	}
}
