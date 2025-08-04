package net.openid.conformance.vci10issuer;

import net.openid.conformance.variant.VariantParameter;

@VariantParameter(
	name = "client_auth_type",
	displayName = "Client Authentication Type",
	description = "The type of client authentication your authorization server supports. If you support multiple types of client authentication test each one, one at a time."
)
public enum VCIClientAuthType {

	MTLS,
	PRIVATE_KEY_JWT,
	CLIENT_ATTESTATION;

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
