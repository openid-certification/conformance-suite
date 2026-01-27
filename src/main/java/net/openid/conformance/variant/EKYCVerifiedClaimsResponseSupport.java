package net.openid.conformance.variant;

@VariantParameter(
	name = "ekyc_verified claims_response_support",
	displayName = "EKYC Verified Claims Response Support",
	description = "Configures whether eKYC verified claims can be returned from ID Token, UserInfo endpoint, or both",
	defaultValue = "id_token_userinfo"
)
public enum EKYCVerifiedClaimsResponseSupport {
	ID_TOKEN,
	USERINFO,
	ID_TOKEN_USERINFO;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
