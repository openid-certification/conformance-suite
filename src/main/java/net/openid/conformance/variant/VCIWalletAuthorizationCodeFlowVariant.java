package net.openid.conformance.variant;

@VariantParameter(
	name = "vci_authorization_code_flow_variant",
	displayName = "Authorization Code Flow Variant",
	description = "VCI Authorization Code Flow Variant to be used.'wallet_initiated' is the most commonly used. If in doubt, select 'wallet_initiated'.",
	defaultValue = "wallet_initiated"
)
public enum VCIWalletAuthorizationCodeFlowVariant {

	WALLET_INITIATED,

	ISSUER_INITIATED,

	// TODO check DC API support once https://github.com/openid/OpenID4VCI/pull/476 is in
	ISSUER_INITIATED_DC_API
	;

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
