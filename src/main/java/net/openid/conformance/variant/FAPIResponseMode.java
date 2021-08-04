package net.openid.conformance.variant;

@VariantParameter(
	name = "fapi_response_mode",
	displayName = "FAPI Response Mode",
	description = "The response mode that will be used. 'Plain response' is the most commonly used. JARM (JWT Secured Authorization Response Mode) is an option in the FAPI specifications. If in doubt, select 'plain_response'."
)
public enum FAPIResponseMode {

	PLAIN_RESPONSE,
	JARM;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
