package net.openid.conformance.variant;

import java.util.List;

@VariantParameter("response_mode")
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
