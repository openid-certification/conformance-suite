package net.openid.conformance.condition.client;

import net.openid.conformance.testmodule.Environment;

/**
 * Checks the status list token endpoint returned Content-Type
 * application/statuslist+jwt, per OAuth Status List draft section 8.2.
 *
 * Skips when no status list fetch was performed (credential has no status
 * claim, or the fetch itself failed before a response was recorded).
 */
public class EnsureContentTypeStatusListJwt extends AbstractCheckEndpointContentTypeReturned {

	@Override
	public Environment evaluate(Environment env) {
		if (!env.containsObject("status_list_token_endpoint_response")) {
			log("No status list token endpoint response recorded, skipping content-type check");
			return env;
		}
		return checkContentType(env, "status_list_token_endpoint_response", "headers.", "application/statuslist+jwt");
	}
}
