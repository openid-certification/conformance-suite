package net.openid.conformance.fapi2spfinal;

/**
 * Profile behavior for CBUAE client tests.
 * Requires mTLS everywhere and treats userinfo as the resource endpoint (test ends there).
 */
public class CbuaeClientProfileBehavior extends FAPI2ClientProfileBehavior {

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}

	@Override
	public boolean userInfoIsResourceEndpoint() {
		return true;
	}
}
