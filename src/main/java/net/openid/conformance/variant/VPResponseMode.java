package net.openid.conformance.variant;

@VariantParameter(
	name = "response_mode",
	displayName = "Response Mode",
	description = "The response_mode that will be used."
)
public enum VPResponseMode
{
	DIRECT_POST("direct_post"),
	DIRECT_POST_JWT("direct_post.jwt"),

	W3C_DC_API("w3c_dc_api"),
	W3C_DC_API_JWT("w3c_dc_api.jwt");

	private final String modeValue;

	private VPResponseMode(String responseMode) {
		modeValue = responseMode;
	}

	@Override
	public String toString() {
		return modeValue;
	}
}
