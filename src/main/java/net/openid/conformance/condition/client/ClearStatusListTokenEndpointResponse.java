package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

/**
 * Clears any status list state left over from a previous credential so split
 * status list validation conditions do not consume stale data.
 */
public class ClearStatusListTokenEndpointResponse extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		env.removeObject("status_list_token_endpoint_response");
		env.removeObject("status_list_token");
		env.removeNativeValue("status_list_idx");
		env.removeNativeValue("status_list_uri");
		logSuccess("Cleared any previous status list validation state");
		return env;
	}
}
