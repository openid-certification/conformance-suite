package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckTokenEndpointReturnedJsonContentType extends AbstractCheckEndpointContentTypeReturned {

	@Override
	@PreEnvironment(required = "token_endpoint_response_headers")
	public Environment evaluate(Environment env) {
		return checkContentType(env, "token_endpoint_response_headers", "application/json");
	}

}
