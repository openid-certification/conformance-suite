package net.openid.conformance.variant;

@VariantParameter(
	name = "vci_access_token_issuance_mode",
	displayName = "VCI Access Token issuance mode",
	description = "VCI Access Token issuance mode to be used.'wallet_initiated' is the most commonly used. If in doubt, select 'wallet_initiated'.",
	defaultValue = "wallet_initiated"
)
public enum OID4VCIAccessTokenIssuanceMode {

	WALLET_INITIATED,

	ISSUER_INITIATED;

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
