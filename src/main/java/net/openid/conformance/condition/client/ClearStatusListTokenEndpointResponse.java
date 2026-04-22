package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

/**
 * Clears any status_list_token_endpoint_response left over from a previous
 * credential so downstream header checks do not see stale state when this
 * credential has no status claim (or the fetch throws before a response is
 * stored).
 */
public class ClearStatusListTokenEndpointResponse extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		env.removeObject("status_list_token_endpoint_response");
		logSuccess("Cleared any previous status_list_token_endpoint_response");
		return env;
	}
}
