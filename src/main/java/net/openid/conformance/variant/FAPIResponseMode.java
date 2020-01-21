package net.openid.conformance.variant;

@VariantParameter(
	name = "fapi_response_mode",
	displayName = "FAPI Response Mode",
	description = "The response mode that will be used. 'Plain response' is required for FAPI certification. JARM responses are not currently part of the certification program. If in doubt, select 'plain_response'."
)
public enum FAPIResponseMode {

	PLAIN_RESPONSE,
	JARM;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
