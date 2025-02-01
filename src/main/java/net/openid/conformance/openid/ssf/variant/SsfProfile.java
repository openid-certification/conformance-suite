package net.openid.conformance.openid.ssf.variant;

import net.openid.conformance.variant.VariantParameter;

@VariantParameter(
	name = "ssf_profile",
	displayName = "SSF Profile",
	description = "Which SSF Profile to run the tests with."
)
public enum SsfProfile {

	CAEP_INTEROP;

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
