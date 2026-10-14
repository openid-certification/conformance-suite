package net.openid.conformance.authzen.condition;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Requests are made with POST (Section 10.1). The AuthZEN working group's guidance for a request
 * with another method is to answer 405, preferably, or 400, and to fall back to 404 only when the
 * server framework routes on method and path. This flags a 404 so the tester can check that it
 * is that fallback rather than a routing problem. Intended to be called at WARNING.
 */
public class WarnMethodRejectedWithNotFound extends AbstractAuthzenResponseHeaderCondition {

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_response")
	public Environment evaluate(Environment env) {
		Integer status = responseStatus(env);
		if (status != null && status == 404) {
			throw error("The PDP answered a request with an unsupported method with 404. 405 (with an Allow: POST header) is preferred, or 400; "
				+ "404 is only expected when the server framework routes on method and path, so check that the endpoint URL is right",
				args("status", status));
		}
		logSuccess("The PDP did not answer the request with an unsupported method with 404", args("status", status));
		return env;
	}
}
