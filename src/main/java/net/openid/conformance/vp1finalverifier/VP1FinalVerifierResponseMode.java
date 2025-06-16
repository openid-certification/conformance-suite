package net.openid.conformance.vp1finalverifier;

import net.openid.conformance.variant.VariantParameter;

@VariantParameter(
	name = "response_mode",
	displayName = "Response Mode",
	description = "The response_mode that will be used."
)
public enum VP1FinalVerifierResponseMode
{
	DIRECT_POST("direct_post"),
	DIRECT_POST_JWT("direct_post.jwt");

	private final String modeValue;

	private VP1FinalVerifierResponseMode(String responseMode) {
		modeValue = responseMode;
	}

	@Override
	public String toString() {
		return modeValue;
	}
}
