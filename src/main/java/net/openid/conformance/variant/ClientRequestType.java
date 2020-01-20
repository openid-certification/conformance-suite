package net.openid.conformance.variant;

import java.util.Locale;

@VariantParameter(
	name = "request_type",
	displayName = "Request Type",
	description = ""
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
