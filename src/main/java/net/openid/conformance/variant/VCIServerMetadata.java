package net.openid.conformance.variant;

@VariantParameter(
	name = "vci_credential_issuer_metadata",
	displayName = "Metadata discovery",
	description = "Whether the server supports discovery (i.e. has a '/.well-known/openid-credential-issuer') or requires the endpoints to be statically configured. If your server supports discovery then it is recommended to use dynamic - it means less manual actions are required to run the tests.",
	defaultValue = "discovery"
)
public enum VCIServerMetadata {

	STATIC,
	DISCOVERY;

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
