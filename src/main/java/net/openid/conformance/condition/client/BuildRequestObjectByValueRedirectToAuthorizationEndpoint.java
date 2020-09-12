package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class BuildRequestObjectByValueRedirectToAuthorizationEndpoint extends AbstractBuildRequestObjectRedirectToAuthorizationEndpoint {

	@Override
	@PreEnvironment(required = { "authorization_endpoint_request", "request_object_claims", "server" }, strings = "request_object")
	@PostEnvironment(strings = "redirect_to_authorization_endpoint")
	public Environment evaluate(Environment env) {
		String requestObject = env.getString("request_object");

		return buildRedirect(env, "request", requestObject, true);
	}

}
