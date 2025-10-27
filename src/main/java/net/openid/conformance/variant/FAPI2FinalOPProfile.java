package net.openid.conformance.variant;

@VariantParameter(
	name = "fapi_profile",
	displayName = "FAPI Profile",
	description = "The FAPI sub-profile to use. If the server is compliant to the OpenBanking UK specifications (i.e. requires the client credentials grant is used and an intent id created) please pick the openbanking_uk option. For servers compliant with the Australian Consumer Data Right standards, please pick consumerdataright_au and also select private_key_jwt client authentication. If in doubt select plain_fapi."
)
public enum FAPI2FinalOPProfile {

	PLAIN_FAPI,
	// as per https://github.com/OpenBankingUK/read-write-api-docs-pub/tree/master/docs/v3.1.8/profiles
	OPENBANKING_UK,
	// as per https://consumerdatastandardsaustralia.github.io/standards/
	CONSUMERDATARIGHT_AU,
	// as per https://github.com/OpenBanking-Brasil/specs-seguranca/
	OPENBANKING_BRAZIL,
	// https://connectid.com.au
	CONNECTID_AU,
	// https://openfinanceuae.atlassian.net/wiki/spaces/StandardsDraft01/pages/39158001/Security+Profile+-+FAPI
	CBUAE,
	// PLAIN_FAPI utilising client credentials grant only.
	FAPI_CLIENT_CREDENTIALS_GRANT;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
