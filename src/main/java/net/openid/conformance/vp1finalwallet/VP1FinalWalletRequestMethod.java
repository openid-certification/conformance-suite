package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.variant.VariantParameter;

@VariantParameter(
	name = "request_method",
	displayName = "Request Method",
	description = "Whether the authorization request to the Wallet is send via a unsigned or signed request_uri. For the W3C DC API, this controls if the request is a JSON object or a signed JWT."
)
public enum VP1FinalWalletRequestMethod
{
	// URL_QUERY("url_query"),
	REQUEST_URI_UNSIGNED("request_uri_unsigned"),
	REQUEST_URI_SIGNED("request_uri_signed");

	private final String requestMethod;

	private VP1FinalWalletRequestMethod(String requestMethodIn) {
		requestMethod = requestMethodIn;
	}

	@Override
	public String toString() {
		return requestMethod;
	}
}
