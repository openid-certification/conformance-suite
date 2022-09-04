package net.openid.conformance.condition.client;

import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class CallUserInfoEndpointWithBearerTokenInBody extends CallUserInfoEndpoint {

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

}
