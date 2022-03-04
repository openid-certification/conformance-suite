package net.openid.conformance.variant;

@VariantParameter(
	name = "sender_constrain",
	displayName = "Sender Constraining",
	description = "The method to use to sender constrain access tokens. FAPI2 allows the use of MTLS or DPoP as proof-of-possession methods, select the one you support. MTLS was the mechanism used in FAPI1. If your software supports both, test both one at a time."
)
public enum FAPI2SenderConstrainMethod {
	MTLS,
	DPOP;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
