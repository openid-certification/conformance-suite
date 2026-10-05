package net.openid.conformance.condition.client;

import com.nimbusds.jose.JOSEObjectType;

/**
 * Signs the request object exactly as {@link SignRequestObjectIncludeX5cHeaderIfAvailable} does,
 * but with no typ header. Used by negative tests.
 */
public class SignRequestObjectIncludeX5cHeaderIfAvailableWithoutTyp extends SignRequestObjectIncludeX5cHeaderIfAvailable {

	@Override
	protected JOSEObjectType getMediaType() {
		return null;
	}

}
