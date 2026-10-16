package net.openid.conformance.condition.client;

import com.nimbusds.jose.JOSEObjectType;

/**
 * Signs the request object exactly as {@link SignRequestObjectIncludeTypHeader} does, but with no
 * typ header. Used by negative tests.
 */
public class SignRequestObjectIncludeTypHeaderWithoutTyp extends SignRequestObjectIncludeTypHeader {

	@Override
	protected JOSEObjectType getMediaType() {
		return null;
	}

}
