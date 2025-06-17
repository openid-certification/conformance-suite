package net.openid.conformance.vpid3verifier;

import net.openid.conformance.variant.VariantParameter;

@VariantParameter(
	name = "request_method",
	displayName = "Request Method",
	description = "Whether the authorization request to the Wallet is send in the URL query directly, or via a unsigned or signed request_uri."
)
public enum VPID3VerifierRequestMethod
{
	// URL_QUERY("url_query"),
//	REQUEST_URI_UNSIGNED("request_uri_unsigned"),
	REQUEST_URI_SIGNED("request_uri_signed");

	private final String requestMethod;

	private VPID3VerifierRequestMethod(String requestMethodIn) {
		requestMethod = requestMethodIn;
	}

	@Override
	public String toString() {
		return requestMethod;
	}
}
