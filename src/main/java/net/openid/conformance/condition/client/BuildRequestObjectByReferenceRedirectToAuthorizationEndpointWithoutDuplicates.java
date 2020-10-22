package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates extends AbstractBuildRequestObjectRedirectToAuthorizationEndpoint {

	@Override
	@PreEnvironment(required = { "authorization_endpoint_request", "request_object_claims", "server" }, strings = "request_uri")
	@PostEnvironment(strings = "redirect_to_authorization_endpoint")
	public Environment evaluate(Environment env) {
		String requestUri = env.getString("request_uri");

		return buildRedirect(env, "request_uri", requestUri, false);
	}

}
