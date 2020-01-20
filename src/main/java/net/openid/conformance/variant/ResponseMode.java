package net.openid.conformance.variant;

@VariantParameter(
	name = "response_mode",
	displayName = "Response Mode",
	description = "The Response Mode request parameter response_mode informs the Authorization Server of the mechanism to be used for returning Authorization Response parameters from the Authorization Endpoint."
)
public enum ResponseMode
{
	/**
	 * default mode for the response type
	 */
	DEFAULT("default"),
	FORM_POST("form_post");

	private String modeValue;

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
