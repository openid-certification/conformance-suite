package net.openid.conformance.variant;

@VariantParameter(
	name = "fapir_client_auth_type",
	displayName = "FAPI-R Client Authentication Type",
	description = "Client Authentication methods that are used by Clients to authenticate to the Authorization Server."
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
