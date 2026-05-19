package net.openid.conformance.fapi2spfinal;

/**
 * Profile behavior for Consumer Data Right Australia client tests.
 * Requires mTLS everywhere; otherwise uses base-class defaults.
 */
public class ConsumerDataRightAuClientProfileBehavior extends FAPI2ClientProfileBehavior {

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}
}
