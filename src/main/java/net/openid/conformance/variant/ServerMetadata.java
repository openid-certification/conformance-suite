package net.openid.conformance.variant;

@VariantParameter(
	name = "server_metadata",
	displayName = "Server metadata location",
	description = "Whether the server supports discovery (i.e. has a '/.well-known/openid-configuration') or requires the endpoints to be statically configured. If your server supports discovery then it is recommended to use dynamic - it means less manual actions are required to run the tests."
)
public enum ServerMetadata {

	STATIC,
	DISCOVERY;

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
