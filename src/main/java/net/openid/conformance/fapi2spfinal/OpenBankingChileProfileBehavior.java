package net.openid.conformance.fapi2spfinal;

/**
 * Profile behavior for Chile Open Finance.
 * Only differs from plain FAPI in requiring mTLS everywhere.
 */
public class OpenBankingChileProfileBehavior extends FAPI2ProfileBehavior {

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}
}
