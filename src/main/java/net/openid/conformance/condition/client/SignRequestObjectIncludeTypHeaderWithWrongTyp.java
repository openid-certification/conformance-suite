package net.openid.conformance.condition.client;

import com.nimbusds.jose.JOSEObjectType;

/**
 * Signs the request object exactly as {@link SignRequestObjectIncludeTypHeader} does, but with the
 * typ header set to 'jwt' instead of 'oauth-authz-req+jwt'. Used by negative tests.
 */
public class SignRequestObjectIncludeTypHeaderWithWrongTyp extends SignRequestObjectIncludeTypHeader {

	@Override
	protected JOSEObjectType getMediaType() {
		return new JOSEObjectType("jwt");
	}

}
