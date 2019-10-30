package net.openid.conformance.variant;

@VariantParameter("client_registration")
public enum ClientRegistration {

	STATIC_CLIENT,
	DYNAMIC_CLIENT;

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
