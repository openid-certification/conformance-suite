package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractCheckEndpointContentTypeReturned;
import net.openid.conformance.testmodule.Environment;

public class EnsureContentTypeEntityStatementJwt extends AbstractCheckEndpointContentTypeReturned {

	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {

		env = checkContentType(env, "endpoint_response", "headers.", "application/entity-statement+jwt");

		return env;
	}

}
