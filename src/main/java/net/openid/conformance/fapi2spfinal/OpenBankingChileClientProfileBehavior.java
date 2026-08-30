package net.openid.conformance.fapi2spfinal;

/**
 * Profile behavior for Chile Open Finance client tests.
 * Requires mTLS everywhere; otherwise uses base-class defaults.
 */
public class OpenBankingChileClientProfileBehavior extends FAPI2ClientProfileBehavior {

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}
}
