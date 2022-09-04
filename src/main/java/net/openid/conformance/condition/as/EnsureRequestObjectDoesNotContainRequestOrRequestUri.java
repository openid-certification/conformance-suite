package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * OIDCC 6.1:
 * request and request_uri parameters MUST NOT be included in Request Objects.
 */
public class EnsureRequestObjectDoesNotContainRequestOrRequestUri extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "authorization_request_object" })
	public Environment evaluate(Environment env) {
		String requestClaim = env.getString("authorization_request_object", "claims.request");
		String requestUriClaim = env.getString("authorization_request_object", "claims.request_uri");

		if (requestClaim!=null && requestUriClaim!=null) {
			throw error("request and request_uri parameters MUST NOT be included in Request Objects",
					args("request", requestClaim, "request_uri", requestUriClaim));
		} else if (requestClaim!=null) {
			throw error("request parameter MUST NOT be included in Request Objects",
				args("request", requestClaim));
		} else if (requestUriClaim!=null) {
			throw error("request_uri parameter MUST NOT be included in Request Objects",
				args("request_uri", requestUriClaim));
		} else {
			logSuccess("Request object does not contain request or request_uri");
			return env;
		}

	}

}
