package net.openid.conformance.variant;

@VariantParameter(
	name = "fapi_profile",
	displayName = "FAPI Profile",
	description = "The FAPI sub-profile to use. If the server is compliant to the OpenBanking UK specifications (i.e. requires the client credentials grant is used and an intent id created) please pick the openbanking_uk option. For servers compliant with the Australian Consumer Data Right standards, please pick consumerdataright_au and also select private_key_jwt client authentication. If in doubt select plain_fapi."
)
public enum FAPI1FinalOPProfile {

	PLAIN_FAPI,
	// as per https://github.com/OpenBankingUK/read-write-api-docs-pub/tree/master/docs/v3.1.8/profiles
	OPENBANKING_UK,
	// as per https://consumerdatastandardsaustralia.github.io/standards/
	CONSUMERDATARIGHT_AU,
	// as per https://github.com/OpenBanking-Brasil/specs-seguranca/
	OPENBANKING_BRAZIL,
	// as per https://github.com/br-openinsurance/areadesenvolvedor/blob/develop/documentation/source/files/swagger/consents.yaml
	OPENINSURANCE_BRAZIL,
	OPENBANKING_KSA;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
