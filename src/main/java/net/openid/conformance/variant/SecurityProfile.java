package net.openid.conformance.variant;

@VariantParameter(
	name = "security_profile",
	displayName = "Security Profile",
	description = "Security Profile specification that tests will follow"
)
public enum SecurityProfile {
	OPENID_CONNECT,
	FAPI1_BASE,
	FAPI1_ADVANCED,
	FAPI2_SP,
	FAPI2_MS;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
