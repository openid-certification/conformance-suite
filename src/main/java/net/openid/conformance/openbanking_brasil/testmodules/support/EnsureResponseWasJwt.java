package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class EnsureResponseWasJwt extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		String contentTypeStr = env.getString("resource_endpoint_response_headers", "content-type");
		if(!contentTypeStr.contains("application/jwt")) {
			throw error("Was expecting a JWT response. Returned: " + contentTypeStr);
		}
		return env;
	}

}

