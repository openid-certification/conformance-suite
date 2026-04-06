package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.variant.VariantParameter;

@VariantParameter(
	name = "request_method",
	displayName = "Request Method",
	description = "Whether the authorization request to the Wallet is sent via an unsigned, signed, or multi-signed request. For the W3C DC API, this controls if the request is a JSON object, a signed JWT, or a JWS JSON Serialization with multiple signatures."
)
public enum VP1FinalWalletRequestMethod
{
	URL_QUERY("url_query"),
	REQUEST_URI_UNSIGNED("request_uri_unsigned"),
	REQUEST_URI_SIGNED("request_uri_signed"),
	REQUEST_URI_MULTISIGNED("request_uri_multisigned");

	private final String requestMethod;

	private VP1FinalWalletRequestMethod(String requestMethodIn) {
		requestMethod = requestMethodIn;
	}

	@Override
	public String toString() {
		return requestMethod;
	}
}
