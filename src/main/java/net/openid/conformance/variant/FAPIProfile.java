package net.openid.conformance.variant;

@VariantParameter(
	name = "fapi_profile",
	displayName = "FAPI Profile",
	description = "The FAPI sub-profile to use. If the server is compliant to the OpenBanking UK specifications (i.e. requires the client credentials grant is used and an intent id created) please pick the openbanking_uk option, otherwise select plain_fapi. If in doubt select plain_fapi."
)
public enum FAPIProfile {

	PLAIN_FAPI,
	OPENBANKING_UK;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
