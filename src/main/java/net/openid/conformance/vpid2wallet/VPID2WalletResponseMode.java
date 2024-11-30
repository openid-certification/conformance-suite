package net.openid.conformance.vpid2wallet;

import net.openid.conformance.variant.VariantParameter;

@VariantParameter(
	name = "response_mode",
	displayName = "Response Mode",
	description = "The response_mode that will be used."
)
public enum VPID2WalletResponseMode
{
	DIRECT_POST("direct_post"),
	DIRECT_POST_JWT("direct_post.jwt"),

	W3C_DC_API("w3c_dc_api"),
	W3C_DC_API_JWT("w3c_dc_api.jwt");

	private final String modeValue;

	private VPID2WalletResponseMode(String responseMode) {
		modeValue = responseMode;
	}

	@Override
	public String toString() {
		return modeValue;
	}
}
