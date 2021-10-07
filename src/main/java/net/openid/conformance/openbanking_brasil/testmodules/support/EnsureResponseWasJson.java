package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class EnsureResponseWasJson extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		String contentTypeStr = env.getString("resource_endpoint_response_headers", "content-type");
		if(!contentTypeStr.contains("application/json")) {
			throw error("Was expecting a JSON response. Returned: " + contentTypeStr);
		}
		return env;
	}

}
