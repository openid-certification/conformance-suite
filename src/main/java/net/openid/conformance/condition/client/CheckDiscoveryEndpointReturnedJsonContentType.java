package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckDiscoveryEndpointReturnedJsonContentType extends AbstractCheckEndpointContentTypeReturned {

	@Override
	@PreEnvironment(required = "discovery_endpoint_response")
	public Environment evaluate(Environment env) {

		env = checkContentType(env, "discovery_endpoint_response", "headers.", "application/json");

		return env;
	}

}
