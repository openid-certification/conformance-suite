package net.openid.conformance.variant;

@VariantParameter(
	name = "fapi_request_method",
	displayName = "Request Method",
	description = "The method to use to pass the request to the authorization server's pushed request object endpoint. If unsure, pick 'unsigned' - the default method in FAPI2 Baseline. Some ecosystems may require the use of FAPI1Advanced compatible signed request objects - in that case, select 'signed non repudiation'."
)
public enum FAPI2AuthRequestMethod {
	UNSIGNED,
	SIGNED_NON_REPUDIATION;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
