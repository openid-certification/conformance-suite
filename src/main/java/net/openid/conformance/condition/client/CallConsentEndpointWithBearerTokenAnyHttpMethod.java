package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpMethod;


public class CallConsentEndpointWithBearerTokenAnyHttpMethod extends CallConsentEndpointWithBearerToken {

	@Override
	protected String getUri(Environment env) {
		HttpMethod httpMethod = getMethod(env);
		if (!httpMethod.equals(HttpMethod.POST)) {
			String consentUrl = env.getString("consent_url");
			if (Strings.isNullOrEmpty(consentUrl)) {
				throw error("consent url missing from configuration");
			}
			return consentUrl;
		}

		return super.getUri(env);
	}

	@Override
	protected HttpMethod getMethod(Environment env) {

		String method = env.getString("http_method");
		if (Strings.isNullOrEmpty(method)) {
			throw error("HTTP method not found");
		}
		return HttpMethod.valueOf(method);
	}

	@Override
	protected Object getBody(Environment env) {
		HttpMethod httpMethod = getMethod(env);
		if (httpMethod.equals(HttpMethod.GET) || httpMethod.equals(HttpMethod.DELETE)) {
			return null;
		}
		return env.getObject("consent_endpoint_request").toString();
	}

	@Override
	@PreEnvironment(required = { "access_token", "resource", "consent_endpoint_request", "resource_endpoint_request_headers" })
	@PostEnvironment(required = { "resource_endpoint_response_headers", "consent_endpoint_response", "consent_endpoint_response_full" })
	public Environment evaluate(Environment env) { return callProtectedResource(env); }
}
