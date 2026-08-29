package net.openid.conformance.condition.client;

import net.openid.conformance.testmodule.Environment;

/**
 * Checks the MSO revocation list endpoint returned Content-Type
 * application/identifierlist+cwt, per ISO/IEC 18013-5 12.3.6.4 ("The identifier list content-type
 * shall be 'application/identifierlist+cwt' further following the requirements as defined in
 * section 8.2 of Token Status List specification").
 *
 * Skips when no identifier list was fetched (the MSO carried no identifier_list element, or the
 * fetch failed before a response was recorded).
 */
public class EnsureContentTypeIdentifierListCwt extends AbstractCheckEndpointContentTypeReturned {

	@Override
	public Environment evaluate(Environment env) {
		if (!env.containsObject(AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_RESPONSE)) {
			log("No identifier list endpoint response recorded, skipping content-type check");
			return env;
		}
		return checkContentType(env, AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_RESPONSE,
			"headers.", AbstractIdentifierListCwtCondition.IDENTIFIER_LIST_CWT_CONTENT_TYPE);
	}
}
