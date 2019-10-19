package net.openid.conformance.variant;

@VariantParameter("fapir_client_auth_type")
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
