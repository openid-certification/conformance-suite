package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsureHttpResponseBodyIsEmpty extends AbstractCondition {

	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {
		var body = env.getElementFromObject("endpoint_response", "body");
		if (body != null && !body.isJsonNull() && !OIDFJSON.getString(body).isEmpty()) {
			throw error("Response body was not empty", args("body", body));
		}
		logSuccess("Response body was correctly empty");
		return env;
	}
}
