package net.openid.conformance.vp1finalverifier;

import net.openid.conformance.variant.VariantParameter;

@VariantParameter(
	name = "client_id_prefix",
	displayName = "Client Id Prefix",
	description = "The Client Identifier Prefix your software supports."
)
public enum VP1FinalVerifierClientIdPrefix {

	REDIRECT_URI,
	X509_SAN_DNS,
	X509_HASH;

	/** Whether a request from a verifier with this prefix is signed with a certificate carried in its x5c header. */
	public boolean usesX5cCertificate() {
		return this == X509_SAN_DNS || this == X509_HASH;
	}

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
