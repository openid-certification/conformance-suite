package net.openid.conformance.fapi2spfinal;

/**
 * Profile behavior for KSA client tests.
 * Requires mTLS everywhere.
 */
public class KsaClientProfileBehavior extends FAPI2ClientProfileBehavior {

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}
}
