package net.openid.conformance.condition.client;

import net.openid.conformance.oauth.statuslists.StatusListCwt;
import net.openid.conformance.testmodule.Environment;

/**
 * Checks the MSO revocation list endpoint returned Content-Type
 * application/statuslist+cwt, per draft-ietf-oauth-status-list section 8.2 as referenced by
 * ISO/IEC 18013-5 12.3.6.3.
 *
 * The caller skips this when no revocation list was retrieved for this credential.
 */
public class EnsureContentTypeMdocRevocationListCwt extends AbstractCheckEndpointContentTypeReturned {

	@Override
	public Environment evaluate(Environment env) {
		return checkContentType(env, AbstractRevocationListCwtCondition.ENV_RESPONSE, "headers.",
			StatusListCwt.CONTENT_TYPE);
	}
}
