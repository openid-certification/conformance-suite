package net.openid.conformance.vpid3wallet;

import net.openid.conformance.variant.VariantParameter;

@VariantParameter(
	name = "client_id_scheme",
	displayName = "Client Id Scheme",
	description = "The client_id_scheme your software supports. Please use 'web_origin' when using DC API with unsigned requests."
)
public enum VPID3WalletClientIdScheme {

	DID("did"),
	PRE_REGISTERED("pre_registered"),
	REDIRECT_URI("redirect_uri"),
	WEB_ORIGIN("web-origin"),
	X509_SAN_DNS("x509_san_dns");

	private final String value;

	private VPID3WalletClientIdScheme(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}

}
