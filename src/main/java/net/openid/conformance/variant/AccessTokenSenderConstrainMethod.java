package net.openid.conformance.variant;

@VariantParameter(
	name = "sender_constrain",
	displayName = "Sender Constraining",
	description = "The method to use to sender constrain access tokens. Supports None, MTLS or DPoP as proof-of-possession methods. If your software supports multiple options, test them one at a time."
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
