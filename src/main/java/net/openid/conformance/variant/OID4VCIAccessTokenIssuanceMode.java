package net.openid.conformance.variant;

@VariantParameter(
	name = "vci_access_token_issuance_mode",
	displayName = "VCI Access Token issuance mode",
	description = "VCI Access Token issuance mode to be used.'authorization_code' is the most commonly used. If in doubt, select 'authorization_code'.",
	defaultValue = "authorization_code"
)
public enum OID4VCIAccessTokenIssuanceMode {

	AUTHORIZATION_CODE,

	PRE_AUTHORIZATION_CODE;

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
