package net.openid.conformance.openid.ssf.variant;

import net.openid.conformance.variant.VariantParameter;

@VariantParameter(
	name = "ssf_server_metadata",
	displayName = "SSF Server metadata location",
	description = "Whether the server supports discovery (i.e. has a '/.well-known/ssf-configuration') or requires the endpoints to be statically configured. If your server supports discovery then it is recommended to use dynamic - it means less manual actions are required to run the tests."
)
public enum SsfServerMetadata {

	STATIC,
	DISCOVERY;

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
