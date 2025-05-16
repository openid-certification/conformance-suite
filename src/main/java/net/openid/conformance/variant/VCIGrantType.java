package net.openid.conformance.variant;

@VariantParameter(
	name = "vci_grant_type",
	displayName = "VCI Grant Type",
	description = "VCI Grant Type to be used.'authorization_code' is the most commonly used. If in doubt, select 'authorization_code'.",
	defaultValue = "authorization_code"
)
public enum VCIGrantType {

	AUTHORIZATION_CODE,

	PRE_AUTHORIZATION_CODE;

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
