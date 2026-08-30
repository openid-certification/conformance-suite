package net.openid.conformance.condition.common;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Reports an incoming HTTP request to a path the test does not serve. Always fails when called;
 * the caller selects the severity - typically FAILURE, so a stray request (e.g. a wallet probing
 * sub-paths of the request_uri) fails the test, but without ending it: the request is answered
 * with a 404 and the test continues, so the real interaction can still complete and be checked.
 */
public class UnexpectedHttpRequestReceived extends AbstractCondition {

	public static final String ENV_KEY = "unexpected_http_request";

	@Override
	@PreEnvironment(required = ENV_KEY)
	public Environment evaluate(Environment env) {
		String path = env.getString(ENV_KEY, "path");
		throw error("Got an HTTP request to '" + path + "', a path the test does not serve; a 404 response was returned and the test continues",
			args("path", path,
				"method", env.getString(ENV_KEY, "method")));
	}
}
