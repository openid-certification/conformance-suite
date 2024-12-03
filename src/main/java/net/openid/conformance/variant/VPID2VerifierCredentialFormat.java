package net.openid.conformance.variant;

@VariantParameter(
	name = "credential_format",
	displayName = "Credential Format",
	description = "The credential format that will be returned by the Wallet."
)
public enum VPID2VerifierCredentialFormat
{
	SD_JWT_VC("sd_jwt_vc");

	private final String modeValue;

	private VPID2VerifierCredentialFormat(String responseMode) {
		modeValue = responseMode;
	}

	@Override
	public String toString() {
		return modeValue;
	}
}
