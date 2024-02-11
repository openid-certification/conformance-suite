package net.openid.conformance.variant;

@VariantParameter(
	name = "request_object",
	displayName = "Request Object",
	description = "Whether the request object sent to the Wallet is unsigned or signed."
)
public enum VPRequestObject
{
	UNSIGNED("unsigned"),
	SIGNED("signed");

	private final String modeValue;

	private VPRequestObject(String responseMode) {
		modeValue = responseMode;
	}

	@Override
	public String toString() {
		return modeValue;
	}
}
