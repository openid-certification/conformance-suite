package net.openid.conformance.variant;

@VariantParameter(
	name = "client_registration",
	displayName = "Client Registration Type",
	description = "Whether the tests will use pre-configured (static) clients or will dynamically register the clients they need. If your server supports dynamic registration then it is recommended to use dynamic - it means less manual actions are required to run the tests."
)
public enum ClientRegistration {

	STATIC_CLIENT,
	DYNAMIC_CLIENT;

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
