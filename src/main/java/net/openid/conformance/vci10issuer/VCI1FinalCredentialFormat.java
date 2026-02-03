package net.openid.conformance.vci10issuer;

import net.openid.conformance.variant.VariantParameter;

@VariantParameter(
	name = "credential_format",
	displayName = "Credential Format",
	description = "The credential format that will be returned to the Wallet."
)
public enum VCI1FinalCredentialFormat {

	SD_JWT_VC("sd_jwt_vc", "dc+sd-jwt"),

	MDOC("mdoc", "mso_mdoc");

	private final String modeValue;

	private final String credentialFormat;

	VCI1FinalCredentialFormat(String responseMode, String credentialFormat) {
		modeValue = responseMode;
		this.credentialFormat = credentialFormat;
	}

	public String getCredentialFormat() {
		return credentialFormat;
	}

	@Override
	public String toString() {
		return modeValue;
	}
}
