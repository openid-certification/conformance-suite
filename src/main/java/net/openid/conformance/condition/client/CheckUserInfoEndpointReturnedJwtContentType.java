package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckUserInfoEndpointReturnedJwtContentType extends AbstractCheckEndpointContentTypeReturned {

	@Override
	@PreEnvironment(required = "userinfo_endpoint_response_headers")
	public Environment evaluate(Environment env) {
		return checkContentType(env, "userinfo_endpoint_response_headers", "application/jwt");
	}

}
