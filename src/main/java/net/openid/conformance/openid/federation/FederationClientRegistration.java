package net.openid.conformance.openid.federation;

import net.openid.conformance.variant.VariantParameter;

@VariantParameter(
	name = "client_registration",
	displayName = "Client Registration Type",
	description = "Whether the tests will use automatic or explicit client registration."
)
public enum FederationClientRegistration {

	AUTOMATIC,
	EXPLICIT;

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
