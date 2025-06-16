package net.openid.conformance.vp1finalverifier;

import net.openid.conformance.variant.VariantParameter;

@VariantParameter(
	name = "client_id_prefix",
	displayName = "Client Id Prefix",
	description = "The Client Identifier Prefix your software supports."
)
public enum VP1FinalVerifierClientIdPrefix {

	X509_SAN_DNS;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
