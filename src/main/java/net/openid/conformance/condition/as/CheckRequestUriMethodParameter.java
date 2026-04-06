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
				env.putString("request_uri_method", requestUriMethod);
				logSuccess("request_uri_method is 'get'");
				return env;
			}
			if (requestUriMethod.equals("post")) {
				env.putString("request_uri_method", requestUriMethod);
				logSuccess("request_uri_method is 'post'");
				return env;
			}
			throw error("Unknown request_uri_method value", args("request_uri_method", requestUriMethod));
		}

		logSuccess("request_uri_method parameter is not present.");

		return env;
	}

}
