package net.openid.conformance.condition.client;

/**
 * KSA Open Finance security profile: "Shall ensure that the access token expiry is no longer
 * than 10 minutes."
 */
public class KsaValidateAccessTokenExpiresIn extends AbstractValidateAccessTokenExpiresInMaximum {

	@Override
	protected int getMaximumSeconds() {
		return 600;
	}

}
