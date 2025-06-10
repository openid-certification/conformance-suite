package net.openid.conformance.vciid2issuer;

import net.openid.conformance.variant.VariantParameter;

@VariantParameter(
	name = "client_auth_type",
	displayName = "VCI Client Authentication Type",
	description = "The type of client authentication your VCI software supports. If you support multiple types of client authentication test each one, one at a time."
)
public enum VCIID2ClientAuthType {

	MTLS,
	PRIVATE_KEY_JWT,
	CLIENT_ATTESTATION;

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
