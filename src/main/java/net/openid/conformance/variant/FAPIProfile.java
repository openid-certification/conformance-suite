package net.openid.conformance.variant;

@VariantParameter("fapi_profile")
public enum FAPIProfile {

	PLAIN_FAPI,
	OPENBANKING_UK;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
