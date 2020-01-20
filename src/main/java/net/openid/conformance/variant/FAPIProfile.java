package net.openid.conformance.variant;

@VariantParameter(
	name = "fapi_profile",
	displayName = "FAPI Profile",
	description = ""
)
public enum FAPIProfile {

	PLAIN_FAPI,
	OPENBANKING_UK;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
