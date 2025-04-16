package net.openid.conformance.openid.federation.client;

import net.openid.conformance.variant.VariantParameter;

@VariantParameter(
	name = "client_registration",
	displayName = "Client Registration Type",
	description = "Client registration type for establishing trust between an RP and an OP that have no prior explicit configuration or registration between them. "
)
public enum ClientRegistration {

	AUTOMATIC,
	EXPLICIT;

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
