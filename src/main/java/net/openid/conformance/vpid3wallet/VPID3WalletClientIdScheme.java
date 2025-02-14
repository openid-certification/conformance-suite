package net.openid.conformance.vpid3wallet;

import net.openid.conformance.variant.VariantParameter;

@VariantParameter(
	name = "client_id_scheme",
	displayName = "Client Id Scheme",
	description = "The client_id_scheme your software supports. Please use 'web_origin' when using DC API with unsigned requests."
)
public enum VPID3WalletClientIdScheme {

	DID,
	PRE_REGISTERED,
	REDIRECT_URI,
	WEB_ORIGIN,
	X509_SAN_DNS;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
