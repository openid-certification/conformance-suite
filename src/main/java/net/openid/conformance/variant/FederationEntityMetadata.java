package net.openid.conformance.variant;

@VariantParameter(
	name = "server_metadata",
	displayName = "Entity configuration location",
	description = "Discovery is the normal mode, which means that the entity configuration is fetched from the entity's .well-known/openid-federation location. " +
		"Static allows you to configure a static entity configuration instead of fetching it remotely."
)
public enum FederationEntityMetadata {

	STATIC,
	DISCOVERY;

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
