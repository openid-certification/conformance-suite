package net.openid.conformance.authzen.condition;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetAuthzenApiEndpointToResourceSearchEndpoint extends AbstractSetAuthzenApiEndpoint {
	@Override
	@PreEnvironment(required = "pdp")
	@PostEnvironment(strings = "authzen_api_endpoint")
	public Environment evaluate(Environment env) {
		return setAuthzenApiEndpoint(env, "Resource Search", "search_resource_endpoint");
	}

}
