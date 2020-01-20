package net.openid.conformance.variant;

@VariantParameter(
	name = "client_auth_type",
	displayName = "Client Authentication Type",
	description = "Client Authentication methods that are used by Clients to authenticate to the Authorization Server."
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
