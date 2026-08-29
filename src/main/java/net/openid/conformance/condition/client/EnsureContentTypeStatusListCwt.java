package net.openid.conformance.condition.client;

import net.openid.conformance.testmodule.Environment;

/**
 * Checks the MSO revocation list endpoint returned Content-Type
 * application/statuslist+cwt, per draft-ietf-oauth-status-list section 8.2 as referenced by
 * ISO/IEC 18013-5 12.3.6.3.
 *
 * Skips when no MSO revocation list was fetched (the MSO carried no status_list element, or the
 * fetch failed before a response was recorded).
 */
public class EnsureContentTypeStatusListCwt extends AbstractCheckEndpointContentTypeReturned {

	@Override
	public Environment evaluate(Environment env) {
		if (!env.containsObject(AbstractStatusListCwtCondition.ENV_STATUS_LIST_RESPONSE)) {
			log("No MSO revocation list endpoint response recorded, skipping content-type check");
			return env;
		}
		return checkContentType(env, AbstractStatusListCwtCondition.ENV_STATUS_LIST_RESPONSE, "headers.",
			AbstractStatusListCwtCondition.STATUS_LIST_CWT_CONTENT_TYPE);
	}
}
