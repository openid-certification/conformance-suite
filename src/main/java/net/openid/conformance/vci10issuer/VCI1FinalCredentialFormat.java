package net.openid.conformance.vci10issuer;

import net.openid.conformance.variant.VariantParameter;

@VariantParameter(
	name = "credential_format",
	displayName = "Credential Format",
	description = "The credential format that will be returned to the Wallet."
)
public enum VCI1FinalCredentialFormat {

	SD_JWT_VC("sd_jwt_vc"),

	MDOC("mdoc");

	private final String modeValue;

	private VCI1FinalCredentialFormat(String responseMode) {
		modeValue = responseMode;
	}

	@Override
	public String toString() {
		return modeValue;
	}
}
