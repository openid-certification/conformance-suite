package net.openid.conformance.variant;

@VariantParameter(
	name = "response_mode",
	displayName = "Response Mode",
	description = "The response mode that will be tested. 'default' is required for certification, 'form_post' is optional."
)
public enum ResponseMode
{
	/**
	 * default mode for the response type
	 */
	DEFAULT("default"),
	FORM_POST("form_post");

	private final String modeValue;

	private ResponseMode(String responseMode) {
		modeValue = responseMode;
	}

	@Override
	public String toString() {
		return modeValue;
	}

	public boolean isFormPost() {
		return "form_post".equals(modeValue);
	}

}
