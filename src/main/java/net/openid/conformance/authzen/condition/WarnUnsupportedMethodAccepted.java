package net.openid.conformance.authzen.condition;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Requests are made with POST (Section 10.1), a requirement on the request rather than on the
 * PDP, so a PDP that answers a request with another method successfully is flagged rather than
 * failed. Intended to be called at WARNING; any other unexpected status is a failure of the
 * status check.
 */
public class WarnUnsupportedMethodAccepted extends AbstractAuthzenResponseHeaderCondition {

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_response")
	public Environment evaluate(Environment env) {
		Integer status = responseStatus(env);
		String method = env.getString("authzen_api_endpoint_request_method");
		if (status != null && status >= 200 && status < 300) {
			throw error("The PDP accepted a " + method + " request. Section 10.1 makes the API requests HTTPS POST; "
				+ "405 (with an Allow: POST header) is preferred, or 400",
				args("method", method, "status", status));
		}
		logSuccess("The PDP did not accept the " + method + " request", args("method", method, "status", status));
		return env;
	}
}
