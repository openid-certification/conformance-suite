package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.variant.VariantParameter;

@VariantParameter(
	name = "client_id_prefix",
	displayName = "Client Id Prefix",
	description = "The Client Identifier Prefix your software supports. Please use 'web_origin' when using DC API with unsigned requests."
)
public enum VP1FinalWalletClientIdPrefix {

	DID("did"),
	PRE_REGISTERED("pre_registered"),
	REDIRECT_URI("redirect_uri"),
	WEB_ORIGIN("web-origin"),
	X509_SAN_DNS("x509_san_dns");

	private final String value;

	private VP1FinalWalletClientIdPrefix(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}

}
