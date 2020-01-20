package net.openid.conformance.variant;

@VariantParameter(
	name = "client_registration",
	displayName = "Client Registration Type",
	description = "Client registration types that are used to register static/dynamic clients."
)
public enum ClientRegistration {

	STATIC_CLIENT,
	DYNAMIC_CLIENT;

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
