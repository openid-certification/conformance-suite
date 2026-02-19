package net.openid.conformance.variant;

@VariantParameter(
	name = "vci_profile",
	displayName = "VCI Profile",
	description = "The VCI Profile to use for the tests. Defaults to HAIP",
	defaultValue="haip"
)
public enum VCIProfile {

	/**
	 * Plain VCI profile
	 */
	PLAIN_VCI,

	/**
	 * HAIP profile
	 */
	HAIP,
	;

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
