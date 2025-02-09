package net.openid.conformance.variant;

@VariantParameter(
	name = "response_mode",
	displayName = "Response Mode",
	description = "The response_mode that will be used."
)
public enum VPID3VerifierResponseMode
{
	DIRECT_POST("direct_post"),
	DIRECT_POST_JWT("direct_post.jwt");

	private final String modeValue;

	private VPID3VerifierResponseMode(String responseMode) {
		modeValue = responseMode;
	}

	@Override
	public String toString() {
		return modeValue;
	}
}
