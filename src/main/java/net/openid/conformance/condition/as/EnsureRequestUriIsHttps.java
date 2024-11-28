package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureRequestUriIsHttps extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_endpoint_http_request_params"})
	public Environment evaluate(Environment env) {
		String requestUri = env.getString("authorization_endpoint_http_request_params", "request_uri");

		if (requestUri.toLowerCase().startsWith("https://")) {
			logSuccess("request_uri is a https url", args("request_uri", requestUri));
			return env;
		} else {
			throw error("The scheme used in the request_uri value MUST be https",
				args("request_uri", requestUri));
		}
	}
}
