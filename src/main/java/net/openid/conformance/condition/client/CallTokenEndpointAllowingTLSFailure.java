package net.openid.conformance.condition.client;

import com.google.gson.JsonParseException;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import javax.net.ssl.SSLException;
import java.net.SocketException;

public class CallTokenEndpointAllowingTLSFailure extends CallTokenEndpointAndReturnFullResponse {

	@Override
	@PreEnvironment(required = { "server", "token_endpoint_request_form_parameters" })
	// token_endpoint_response_ssl_error should be present in PostEnvironment, but no annotation for that currently
	// "token_endpoint_response" isn't returned if a TLS failure occurs
	public Environment evaluate(Environment env) {
		env.putBoolean("token_endpoint_response_ssl_error", false);
		return super.evaluate(env);
	}

	@Override
	protected Environment handleClientException(Environment env, RestClientException e) {
		if (e instanceof ResourceAccessException &&
			(e.getCause() instanceof SSLException || e.getCause() instanceof SocketException)) {
			env.putBoolean("token_endpoint_response_ssl_error", true);
			logSuccess("Call to token_endpoint failed due to a TLS issue", ex(e));
			return env;
		}
		return super.handleClientException(env, e);
	}

	@Override
	protected Environment handleJsonParseException(Environment env, JsonParseException e) {
		log("token endpoint response parsed but not valid JSON");
		return env;
	}
}
