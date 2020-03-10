package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckCallbackHttpMethodIsPost extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "callback_http_method")
	public Environment evaluate(Environment env) {
		String method = env.getString("callback_http_method");

		if (!method.equals("POST")) {
			throw error("The HTTP method used at redirect_uri is not 'POST'", args("method", method));
		}

		logSuccess("HTTP method used at redirect_uri is 'POST'");
		return env;
	}

}
