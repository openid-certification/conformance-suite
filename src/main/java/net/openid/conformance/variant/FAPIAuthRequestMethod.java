package net.openid.conformance.variant;

@VariantParameter(
	name = "fapi_auth_request_method",
	displayName = "Request Object Method",
	description = "The method to use to pass the request object to the authorization server. Please select 'by_value' unless you know your server supports the 'pushed authorization request' ('PAR') endpoint as defined here: https://datatracker.ietf.org/doc/html/rfc9126"
)
public enum FAPIAuthRequestMethod {
	BY_VALUE,
	PUSHED;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
