package net.openid.conformance.variant;

@VariantParameter(
	name = "ekyc_profile",
	displayName = "EKYC Profile",
	description = "The EKYC sub-profile to use.",
	defaultValue = "plain_ekyc"
)
public enum EKYCProfile {
	PLAIN_EKYC,
	SELECT_ID;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
