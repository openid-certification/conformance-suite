package io.fintechlabs.testframework.variant;

@VariantParameter("client_auth_type")
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
