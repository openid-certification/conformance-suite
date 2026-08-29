package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Checks that the verifier fetched the revocation list with a GET, the only retrieval method
 * draft-ietf-oauth-status-list section 8.1 defines: a Status Provider must return the token in
 * response to an HTTP GET, unless the two parties agreed an alternative distribution method,
 * which a verifier under test has no way of having agreed with this test instance.
 */
public class EnsureRevocationListRequestUsedGet extends AbstractCondition {

	@Override
	@PreEnvironment(required = VP1FinalRevocationListRequest.ENV_KEY)
	public Environment evaluate(Environment env) {

		String method = env.getString(VP1FinalRevocationListRequest.ENV_KEY, "method");

		if (!"GET".equalsIgnoreCase(method)) {
			throw error("The verifier did not use GET to fetch the revocation list the presented credential references",
				args("http_method", method));
		}

		logSuccess("The verifier used GET to fetch the revocation list", args("http_method", method));
		return env;
	}
}
