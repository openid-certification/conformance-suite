package net.openid.conformance.variant;

@VariantParameter(
	name = "auth_request_non_repudiation_method",
	displayName = "Authorization Request Object Non-Repudiation Method",
	description = "The non-repudiation method to use when using request objects to send the authorization request."
)
public enum AuthRequestNonRepudiationMethod {
	UNSIGNED,
	SIGNED_NON_REPUDIATION;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
