package net.openid.conformance.variant;

@VariantParameter(
	name = "vp_profile",
	displayName = "VP Profile",
	description = "The VP Profile to use for the tests. Defaults to HAIP",
	defaultValue="haip"
)
public enum VPProfile {

	PLAIN_VP,

	HAIP,
	;

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
