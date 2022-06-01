package net.openid.conformance.variant;

@VariantParameter(
	name = "fapi_client_type",
	displayName = "FAPI Client Type",
	description = "Indicates the Client type and whether openid scope will be used or not. OIDC should be selected when openid scope is used. Id tokens will not be issued when PLAIN_OAUTH is selected."
)
//TODO instead of this class we should change FAPIResponseMode and have JARM_OAUTH and JARM_OIDC instead of JARM
public enum FAPIClientType
{

	OIDC,
	PLAIN_OAUTH;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
