package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckCallbackHttpMethodIsGet extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "callback_http_method")
	public Environment evaluate(Environment env) {
		String method = env.getString("callback_http_method");

		if (!method.equals("GET")) {
			throw error("The HTTP method used at redirect_uri is not 'GET'", args("method", method));
		}

		logSuccess("HTTP method used at redirect_uri is 'GET'");
		return env;
	}

}
