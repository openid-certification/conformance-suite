package net.openid.conformance.condition;

import net.openid.conformance.testmodule.Environment;

/**
 * Conditions can't call {@code getVariant(...)} the way test modules can, so test modules
 * write a profile marker into env at startup and conditions read it via this helper.
 */
public final class Profile {

	private Profile() {
	}

	public static boolean isHaip(Environment env) {
		return "vci_haip".equals(env.getString("vci", "fapi_profile"))
			|| "haip".equals(env.getString("vp", "vp_profile"));
	}
}
