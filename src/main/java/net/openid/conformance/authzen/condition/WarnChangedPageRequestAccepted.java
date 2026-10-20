package net.openid.conformance.authzen.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Section 8.2: the parameters of a request that carries a page token MUST be identical to the
 * preceding request's, a requirement on the request, and "PDPs SHOULD return an error when any
 * entity or parameter has been changed". A PDP that answers such a request with a page is flagged
 * rather than failed. Intended to be called at WARNING; any other status outside the acceptable
 * 4xx is a failure of the status check.
 */
public class WarnChangedPageRequestAccepted extends AbstractCondition {

	private final String changed;

	/** For a request in which {@code changed}, a parameter such as "page.limit" or an entity such as "action", was changed. */
	public WarnChangedPageRequestAccepted(String changed) {
		this.changed = changed;
	}

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_response")
	public Environment evaluate(Environment env) {
		Integer status = env.getInteger("authzen_api_endpoint_response", "status");
		if (status != null && status == 200) {
			throw error("The PDP returned a page for a request whose " + changed + " differs from the preceding request "
				+ "that returned the page token. Section 8.2 says the PDP SHOULD return an error when any entity or "
				+ "parameter has been changed", args("status", status, "changed", changed));
		}
		logSuccess("The PDP did not return a page for the request with a changed " + changed, args("status", status));
		return env;
	}
}
