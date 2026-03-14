package net.openid.conformance.fapi2spfinal;

/**
 * Profile behavior for CBUAE (Central Bank of UAE).
 * Only differs from plain FAPI in requiring mTLS everywhere.
 */
public class CbuaeProfileBehavior extends FAPI2ProfileBehavior {

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}
}
