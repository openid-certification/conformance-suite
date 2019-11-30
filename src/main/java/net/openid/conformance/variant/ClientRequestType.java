package net.openid.conformance.variant;

import java.util.Locale;

@VariantParameter("request_type")
public enum ClientRequestType {

	PLAIN_HTTP_REQUEST,
	REQUEST_OBJECT,
	REQUEST_URI;

	@Override
	public String toString() {
		return name().toLowerCase(Locale.ENGLISH);
	}
}
