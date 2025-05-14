package net.openid.conformance.variant;

@VariantParameter(
	name = "vci_code_flow_variant",
	displayName = "VCI Code Flow Variant",
	description = "VCI Code Flow Variant to be used.'authorization_code' is the most commonly used. If in doubt, select 'authorization_code'.",
	defaultValue = "authorization_code"
)
public enum VCICodeFlowVariant {

	AUTHORIZATION_CODE,

	PRE_AUTHORIZATION_CODE;

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
