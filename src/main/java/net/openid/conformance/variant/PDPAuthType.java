package net.openid.conformance.variant;

@VariantParameter(
	name = "pdp_auth_type",
	displayName = "PDP Authentication Type",
	description = "The type of client authentication your PDP supports. If you support multiple types of authentication test each one, one at a time."
)
public enum PDPAuthType {

	NONE,
	CLIENT_SECRET_BASIC,
	API_KEY,
	MTLS;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
