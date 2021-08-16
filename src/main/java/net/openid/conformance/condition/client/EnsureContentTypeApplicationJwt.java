package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureContentTypeApplicationJwt extends AbstractCheckEndpointContentTypeReturned {

	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {

		env = checkContentType(env, "endpoint_response", "headers.", "application/jwt");

		return env;
	}

}
