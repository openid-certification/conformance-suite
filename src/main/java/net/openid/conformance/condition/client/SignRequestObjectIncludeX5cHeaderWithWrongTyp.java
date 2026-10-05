package net.openid.conformance.condition.client;

import com.nimbusds.jose.JOSEObjectType;

/**
 * Signs the request object exactly as {@link SignRequestObjectIncludeX5cHeader} does, but with
 * the typ header set to 'jwt' instead of 'oauth-authz-req+jwt'. Used by negative tests: per
 * OID4VP section 5, wallets MUST NOT process request objects whose typ is not 'oauth-authz-req+jwt'.
 */
public class SignRequestObjectIncludeX5cHeaderWithWrongTyp extends SignRequestObjectIncludeX5cHeader {

	@Override
	protected JOSEObjectType getMediaType() {
		return new JOSEObjectType("jwt");
	}

}
