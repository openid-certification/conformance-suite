package net.openid.conformance.variant;

@VariantParameter(
	name = "client_id_scheme",
	displayName = "Client Id Scheme",
	description = "The client_id_scheme your software supports. ."
)
public enum VPID2VerifierClientIdScheme {

	X509_SAN_DNS;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
