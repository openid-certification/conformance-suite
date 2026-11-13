package net.openid.conformance.authzen.condition;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Reads the Search API response in {@code authzen_search_endpoint_response}.
 */
public class WarnAuthzenSearchResponseNullMembers extends AbstractWarnAuthzenNullMembers {

	@Override
	@PreEnvironment(required = "authzen_search_endpoint_response")
	public Environment evaluate(Environment env) {
		return reportNullMembers(env, "authzen_search_endpoint_response", "Search API response");
	}
}
