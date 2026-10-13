package net.openid.conformance.condition.client;

import net.openid.conformance.util.MdocIacaCrl;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Checks the CRL distribution point returned the CRL as application/pkix-crl, the media type
 * RFC 2585 section 4.2 registers for a DER encoded CRL.
 *
 * The caller skips this when no CRL was fetched for this credential.
 */
public class EnsureContentTypeMdocDsCertificateCrl extends AbstractCheckEndpointContentTypeReturned {

	@Override
	@PreEnvironment(required = { AbstractMdocDsCertificateCrlCondition.ENV_RESPONSE })
	public Environment evaluate(Environment env) {
		return checkContentType(env, AbstractMdocDsCertificateCrlCondition.ENV_RESPONSE, "headers.",
			MdocIacaCrl.CONTENT_TYPE);
	}
}
