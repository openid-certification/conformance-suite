package net.openid.conformance.variant;

@VariantParameter(
	name = "grant_management",
	displayName = "Grant Management",
	description = "Whether Grant Management (https://openid.net/specs/fapi-grant-management.html) support is required. When enabled, every authorization request includes grant_management_action=create and the token response is validated to contain a grant_id.",
	defaultValue = "disabled"
)
public enum GrantManagement {

	DISABLED,
	ENABLED;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
