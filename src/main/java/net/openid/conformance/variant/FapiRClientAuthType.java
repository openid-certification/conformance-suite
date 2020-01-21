package net.openid.conformance.variant;

@VariantParameter(
	name = "fapir_client_auth_type",
	displayName = "Client Authentication",
	description = "The type of client authentication your server supports. If you support multiple types of client authentication test each one, one at a time."
)
public enum FapiRClientAuthType {

	NONE,
	CLIENT_SECRET_JWT,
	PRIVATE_KEY_JWT,
	MTLS;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
