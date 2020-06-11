package net.openid.conformance.condition.client;

import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpHeaders;

public abstract class AbstractCallProtectedResourceWithBearerToken extends AbstractCallProtectedResource {

	@Override
	protected HttpHeaders getHeaders(Environment env) {

		HttpHeaders headers = super.getHeaders(env);

		headers.set("Authorization", "Bearer " + getAccessToken(env));

		return headers;
	}
}
