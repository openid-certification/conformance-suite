package net.openid.conformance.variant;

@VariantParameter(
	name = "pdp_server_metadata",
	displayName = "PDP server metadata location",
	description = "Whether the PDP server supports discovery (i.e. has a '/.well-known/authzen-configuration') or requires the endpoints to be statically configured. If your server supports discovery then it is recommended to use dynamic - it means less manual actions are required to run the tests."
)
public enum PDPServerMetadata {

	STATIC,
	DISCOVERY;

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
