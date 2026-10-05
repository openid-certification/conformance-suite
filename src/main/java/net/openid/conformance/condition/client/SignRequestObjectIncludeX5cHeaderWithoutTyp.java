package net.openid.conformance.condition.client;

import com.nimbusds.jose.JOSEObjectType;

/**
 * Signs the request object exactly as {@link SignRequestObjectIncludeX5cHeader} does, but with
 * no typ header. Used by negative tests: per OID4VP section 5, wallets MUST NOT process request
 * objects where the typ header is not present.
 */
public class SignRequestObjectIncludeX5cHeaderWithoutTyp extends SignRequestObjectIncludeX5cHeader {

	@Override
	protected JOSEObjectType getMediaType() {
		return null;
	}

}
