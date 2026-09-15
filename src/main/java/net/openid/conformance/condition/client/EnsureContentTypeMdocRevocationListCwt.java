package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractRevocationListCwtCondition.Mechanism;
import net.openid.conformance.testmodule.Environment;

/**
 * Checks the MSO revocation list endpoint returned the Content-Type of the mechanism the MSO
 * uses: application/statuslist+cwt per draft-ietf-oauth-status-list section 8.2 as referenced
 * by ISO/IEC 18013-5 12.3.6.3, or application/identifierlist+cwt per 12.3.6.4 ("The identifier
 * list content-type shall be 'application/identifierlist+cwt' further following the
 * requirements as defined in section 8.2 of Token Status List specification").
 *
 * The caller skips this when no revocation list was retrieved for this credential.
 */
public class EnsureContentTypeMdocRevocationListCwt extends AbstractCheckEndpointContentTypeReturned {

	@Override
	@PreEnvironment(strings = { AbstractRevocationListCwtCondition.ENV_MECHANISM })
	public Environment evaluate(Environment env) {
		String msoElement = env.getString(AbstractRevocationListCwtCondition.ENV_MECHANISM);
		Mechanism mechanism = Mechanism.fromMsoElement(msoElement);
		if (mechanism == null) {
			throw error("The MSO revocation mechanism is missing from the environment",
				args("mechanism", msoElement));
		}
		return checkContentType(env, AbstractRevocationListCwtCondition.ENV_RESPONSE, "headers.",
			mechanism.contentType);
	}
}
