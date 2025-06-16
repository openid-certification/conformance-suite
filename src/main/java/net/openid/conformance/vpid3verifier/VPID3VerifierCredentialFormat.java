package net.openid.conformance.vpid3verifier;

import net.openid.conformance.variant.VariantParameter;

@VariantParameter(
	name = "credential_format",
	displayName = "Credential Format",
	description = "The credential format that will be returned by the Wallet."
)
public enum VPID3VerifierCredentialFormat
{
	SD_JWT_VC("sd_jwt_vc"),
	ISO_MDL("iso_mdl");

	private final String modeValue;

	private VPID3VerifierCredentialFormat(String responseMode) {
		modeValue = responseMode;
	}

	@Override
	public String toString() {
		return modeValue;
	}
}
