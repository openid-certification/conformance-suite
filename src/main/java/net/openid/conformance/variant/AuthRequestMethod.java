package net.openid.conformance.variant;

@VariantParameter(
	name = "auth_request_method",
	displayName = "Authorization Request Object Method",
	description = "The method to use to pass the request to the authorization server. Please select 'http_query' or 'request_object_by_value' unless you know your server supports the 'pushed authorization request' ('PAR') endpoint as defined here: https://datatracker.ietf.org/doc/html/rfc9126"
)
public enum AuthRequestMethod {
	HTTP_QUERY,
	REQUEST_OBJECT_BY_VALUE,
	REQUEST_OBJECT_PUSHED;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
