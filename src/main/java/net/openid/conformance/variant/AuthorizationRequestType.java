package net.openid.conformance.variant;

@VariantParameter(
		name = "authorization_request_type",
		displayName = "Authorization Request Type",
		description = "The authorization request type to be used.'simple' is the most commonly used. RAR (Rich Authorization Requests) is an alternative to scope authorizations. If in doubt, select 'simple'.",
		defaultValue = "simple"
)
public enum AuthorizationRequestType {

	SIMPLE,
	RAR;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
