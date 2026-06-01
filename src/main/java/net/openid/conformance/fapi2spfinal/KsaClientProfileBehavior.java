package net.openid.conformance.fapi2spfinal;

/**
 * Profile behavior for KSA client tests.
 * Requires mTLS everywhere and treats userinfo as the resource endpoint.
 */
public class KsaClientProfileBehavior extends FAPI2ClientProfileBehavior {

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}

	@Override
	public boolean userInfoIsResourceEndpoint() {
		return true;
	}
}
