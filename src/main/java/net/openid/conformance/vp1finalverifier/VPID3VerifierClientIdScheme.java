package net.openid.conformance.vp1finalverifier;

import net.openid.conformance.variant.VariantParameter;

@VariantParameter(
	name = "client_id_scheme",
	displayName = "Client Id Scheme",
	description = "The client_id_scheme your software supports. ."
)
public enum VPID3VerifierClientIdScheme {

	X509_SAN_DNS;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
