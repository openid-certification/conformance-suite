package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.variant.VariantParameter;

@VariantParameter(
	name = "response_mode",
	displayName = "Response Mode",
	description = "The response_mode that will be used. The '.jwt' options will use an encrypted response. For the W3C Browser Digital Credentials API select one of the 'dc_api' options."
)
public enum VP1FinalWalletResponseMode
{
	DIRECT_POST("direct_post"),
	DIRECT_POST_JWT("direct_post.jwt"),

	DC_API("dc_api"),
	DC_API_JWT("dc_api.jwt");

	private final String modeValue;

	private VP1FinalWalletResponseMode(String responseMode) {
		modeValue = responseMode;
	}

	@Override
	public String toString() {
		return modeValue;
	}
}
