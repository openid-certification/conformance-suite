package net.openid.conformance.variant;

@VariantParameter(
	name = "access_token_sender_constrain",
	displayName = "Access Token Sender Constrain Method",
	description = "The method to use to sender constrain access tokens."
)
public enum AccessTokenSenderConstrainMethod {
	NONE,
	MTLS,
	DPOP;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
