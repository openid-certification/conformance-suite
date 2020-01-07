package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class UserInfoEndpointWithAccessTokenInBodyNotSupported extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		throw error("The server returned a non-2xx HTTP response, and hence does not appear to support access tokens passed in the POST body.");
	}

}
