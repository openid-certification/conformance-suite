package net.openid.conformance.variant;

import java.util.Locale;

@VariantParameter(
	name = "request_type",
	displayName = "Request Type",
	description = "Whether to use standard OAuth2 style requests, request objects (by value) or request_uri (i.e. request object by reference)"
)
public enum ClientRequestType {

	PLAIN_HTTP_REQUEST,
	REQUEST_OBJECT,
	REQUEST_URI;

	@Override
	public String toString() {
		return name().toLowerCase(Locale.ENGLISH);
	}
}
