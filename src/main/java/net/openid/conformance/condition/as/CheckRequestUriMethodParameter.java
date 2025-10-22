package net.openid.conformance.condition.as;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckRequestUriMethodParameter extends AbstractEnsureResponseType {

	@Override
	@PreEnvironment(required = CreateEffectiveAuthorizationRequestParameters.ENV_KEY)
	public Environment evaluate(Environment env) {
		String requestUriMethod = env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "request_uri_method");

		if (requestUriMethod != null) {
			if (requestUriMethod.equals("get")) {
				logSuccess(env.getString("request_uri_method is 'get'"));
				return env;
			}
			throw error("A request_uri_method parameter that is not supported by the conformance suite has been requested. As permitted by the specification the test will continue as if 'get' had been specified.");
		}

		logSuccess("request_uri_method parameter is not present.");

		return env;
	}

}
