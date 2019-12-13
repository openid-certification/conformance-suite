package net.openid.conformance.condition.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CallUserInfoEndpointWithBearerTokenInBody extends CallUserInfoEndpointWithBearerToken {

	@Override
	@PreEnvironment(required = { "access_token", "server" })
	@PostEnvironment(required = "userinfo_endpoint_response_headers", strings = "userinfo_endpoint_response")
	public Environment evaluate(Environment env) {
		return callProtectedResource(env);
	}

	@Override
	protected HttpMethod getMethod(Environment env) {
		return HttpMethod.POST;
	}

	@Override
	protected HttpHeaders getHeaders(Environment env) {

		// Don't add an Authorization header
		return new HttpHeaders();
	}

	@Override
	protected Object getBody(Environment env) {
		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("access_token", getAccessToken(env));
		return body;
	}

	protected Environment handleClientResponseException(Environment env, RestClientResponseException e) {
		int code = e.getRawStatusCode();
		if (code == 405) {
			// Allow the test to be skipped if the endpoint does not support token-in-body per RFC6750-2.2
			env.putInteger("userinfo_endpoint_response_code", code);
			log("Userinfo endpoint returned a \"method not allowed\" error", args("code", code, "status", e.getStatusText()));
			return env;
		} else {
			return super.handleClientResponseException(env, e);
		}
	}
}
