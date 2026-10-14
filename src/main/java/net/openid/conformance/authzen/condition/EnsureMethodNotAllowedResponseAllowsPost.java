package net.openid.conformance.authzen.condition;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;
import java.util.List;

/**
 * RFC 7231 Section 6.5.5: "The origin server MUST generate an Allow header field in a 405
 * response containing a list of the target resource's currently supported methods". AuthZEN
 * endpoints support POST (Section 10.1), and the AuthZEN working group's guidance for a request
 * with another method is a 405 with Allow: POST. Checks only a 405 response. Method tokens are
 * case-sensitive (RFC 9110 Section 9.1), so "post" does not list POST.
 */
public class EnsureMethodNotAllowedResponseAllowsPost extends AbstractAuthzenResponseHeaderCondition {

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_response")
	public Environment evaluate(Environment env) {
		Integer status = responseStatus(env);
		if (status == null || status != 405) {
			log("The response is not a 405, so it needs no Allow header", args("status", status));
			return env;
		}
		List<String> allow = responseHeaderValues(env, "Allow");
		if (allow.isEmpty()) {
			throw error("The PDP answered with 405 but sent no Allow header", args("status", status));
		}
		boolean allowsPost = allow.stream()
			.flatMap(value -> Arrays.stream(value.split(",")))
			.map(String::trim)
			.anyMatch("POST"::equals);
		if (!allowsPost) {
			throw error("The PDP answered with 405 but its Allow header does not list POST", args("allow", allow));
		}
		logSuccess("The 405 response's Allow header lists POST", args("allow", allow));
		return env;
	}
}
