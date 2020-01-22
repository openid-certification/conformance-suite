package net.openid.conformance.variant;

@VariantParameter(
	name = "client_auth_type",
	displayName = "Client Authentication Type",
	description = "The type of client authentication your software supports. If you support multiple types of client authentication test each one, one at a time."
)
public enum ClientAuthType {

	NONE,
	CLIENT_SECRET_BASIC,
	CLIENT_SECRET_POST,
	CLIENT_SECRET_JWT,
	PRIVATE_KEY_JWT,
	MTLS;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
