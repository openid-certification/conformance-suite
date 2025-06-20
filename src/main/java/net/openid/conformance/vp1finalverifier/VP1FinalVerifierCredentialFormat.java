package net.openid.conformance.vp1finalverifier;

import net.openid.conformance.variant.VariantParameter;

@VariantParameter(
	name = "credential_format",
	displayName = "Credential Format",
	description = "The credential format that will be returned by the Wallet."
)
public enum VP1FinalVerifierCredentialFormat
{
	SD_JWT_VC("sd_jwt_vc"),
	ISO_MDL("iso_mdl");

	private final String modeValue;

	private VP1FinalVerifierCredentialFormat(String responseMode) {
		modeValue = responseMode;
	}

	@Override
	public String toString() {
		return modeValue;
	}
}
