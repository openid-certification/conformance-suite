package net.openid.conformance.condition.client;

import com.nimbusds.jose.JOSEObjectType;

/**
 * Creates the multi-signed request object exactly as {@link CreateMultiSignedRequestObject} does,
 * but with no typ in the protected header of any signature.
 */
public class CreateMultiSignedRequestObjectWithoutTyp extends CreateMultiSignedRequestObject {

	@Override
	protected JOSEObjectType getMediaType() {
		return null;
	}

}
