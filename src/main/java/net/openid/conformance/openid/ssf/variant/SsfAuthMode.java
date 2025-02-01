package net.openid.conformance.openid.ssf.variant;

import net.openid.conformance.variant.VariantParameter;

@VariantParameter(
	name = "ssf_auth_mode",
	displayName = "Authentication Variant",
	description = "Whether to use a static access token or obtain one dynamically."
)
public enum SsfAuthMode {

	/**
	 * Use the provided access token
	 */
	STATIC,

	/**
	 * Obtain an access token with client credentials grant
	 */
	DYNAMIC;

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
