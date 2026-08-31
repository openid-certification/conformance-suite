package net.openid.conformance.variant;

@VariantParameter(
	name = "fapi_profile",
	sortOrder = 50,
	displayName = "FAPI Profile",
	description = "The FAPI sub-profile to use. For servers compliant with the Australian Consumer Data Right standards, please pick consumerdataright_au and also select private_key_jwt client authentication. If in doubt select plain_fapi."
)
public enum FAPI2FinalOPProfile {

	PLAIN_FAPI,
	// as per https://consumerdatastandardsaustralia.github.io/standards/
	CONSUMERDATARIGHT_AU,
	// as per https://github.com/OpenBanking-Brasil/specs-seguranca/
	OPENBANKING_BRAZIL,
	// https://connectid.com.au
	CONNECTID_AU,
	// https://openfinanceuae.atlassian.net/wiki/spaces/StandardsDraft01/pages/39158001/Security+Profile+-+FAPI
	CBUAE,
	// https://www.cmfchile.cl/
	OPENBANKING_CHILE,
	// https://openfinance.sa/ (Saudi Arabia)
	KSA,
	// PLAIN_FAPI utilising client credentials grant only.
	FAPI_CLIENT_CREDENTIALS_GRANT,
	// OID4VCI base profile
	VCI,
	// OID4VCI + HAIP profile
	VCI_HAIP;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
