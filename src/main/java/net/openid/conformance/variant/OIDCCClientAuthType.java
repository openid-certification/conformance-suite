package net.openid.conformance.variant;

import java.util.Locale;

@VariantParameter(
	name = "client_auth_type",
	displayName = "Client Authentication Type",
	description = "The type of client authentication your software supports. " +
		"If you support multiple types of client authentication test each one, one at a time."
)
public enum OIDCCClientAuthType
{

	NONE,
	CLIENT_SECRET_BASIC,
	CLIENT_SECRET_POST,
	CLIENT_SECRET_JWT,
	PRIVATE_KEY_JWT,
	TLS_CLIENT_AUTH,
	SELF_SIGNED_TLS_CLIENT_AUTH;

	@Override
	public String toString() {
		return name().toLowerCase(Locale.ENGLISH);
	}

}
