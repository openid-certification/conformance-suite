package net.openid.conformance.condition.client;

import com.nimbusds.jose.JOSEObjectType;

/**
 * Creates the multi-signed request object exactly as {@link CreateMultiSignedRequestObject} does,
 * but with typ 'jwt' instead of 'oauth-authz-req+jwt' in the protected header of every signature,
 * so that whichever signature the wallet validates carries the wrong typ.
 */
public class CreateMultiSignedRequestObjectWithWrongTyp extends CreateMultiSignedRequestObject {

	@Override
	protected JOSEObjectType getMediaType() {
		return new JOSEObjectType("jwt");
	}

}
