package net.openid.conformance.variant;

@VariantParameter(
	name = "fapi_ciba_profile",
	displayName = "FAPI CIBA Profile",
	description = "The FAPI CIBA sub-profile to use."
)
public enum FAPICIBAProfile {

	PLAIN_FAPI,
	// as per https://github.com/OpenBankingUK/read-write-api-docs-pub/tree/master/docs/v3.1.8/profiles
	OPENBANKING_UK,
	// as per https://github.com/OpenBanking-Brasil/specs-seguranca/
	OPENFINANCE_BRAZIL,
	// https://connectid.com.au
	CONNECTID_AU;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
