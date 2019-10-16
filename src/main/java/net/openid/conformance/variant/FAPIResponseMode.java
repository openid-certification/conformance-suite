package net.openid.conformance.variant;

@VariantParameter("fapi_response_mode")
public enum FAPIResponseMode {

	PLAIN_RESPONSE,
	JARM;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
