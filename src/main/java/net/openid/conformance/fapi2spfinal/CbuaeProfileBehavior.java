package net.openid.conformance.fapi2spfinal;

/**
 * CBUAE (Central Bank of the UAE) profile behavior.
 */
public class CbuaeProfileBehavior extends FAPI2ProfileBehavior {

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}
}
