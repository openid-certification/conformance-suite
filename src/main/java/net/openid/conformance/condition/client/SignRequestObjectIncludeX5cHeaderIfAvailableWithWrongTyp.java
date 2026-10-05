package net.openid.conformance.condition.client;

import com.nimbusds.jose.JOSEObjectType;

/**
 * Signs the request object exactly as {@link SignRequestObjectIncludeX5cHeaderIfAvailable} does,
 * but with the typ header set to 'jwt' instead of 'oauth-authz-req+jwt'. Used by negative tests.
 */
public class SignRequestObjectIncludeX5cHeaderIfAvailableWithWrongTyp extends SignRequestObjectIncludeX5cHeaderIfAvailable {

	@Override
	protected JOSEObjectType getMediaType() {
		return new JOSEObjectType("jwt");
	}

}
