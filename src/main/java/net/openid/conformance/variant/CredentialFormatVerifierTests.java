package net.openid.conformance.variant;

@VariantParameter(
	name = "credential_format",
	displayName = "Credential Format",
	description = "The credential format that will be returned by the Wallet."
)
public enum CredentialFormatVerifierTests
{
	SD_JWT_VC("sd_jwt_vc");

	private final String modeValue;

	private CredentialFormatVerifierTests(String responseMode) {
		modeValue = responseMode;
	}

	@Override
	public String toString() {
		return modeValue;
	}
}
