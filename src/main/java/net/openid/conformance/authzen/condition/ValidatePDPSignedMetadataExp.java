package net.openid.conformance.authzen.condition;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidatePDPSignedMetadataExp extends AbstractValidatePDPSignedMetadataTimeClaim {

	@Override
	@PreEnvironment(required = {"pdp_signed_metadata"})
	public Environment evaluate(Environment env) {
		Long exp = getTimeClaim(env, "exp");
		if (exp == null) {
			log("PDP signed_metadata has no exp claim (optional), skipping check");
			return env;
		}

		long now = nowSeconds();
		if (exp < now - CLOCK_SKEW_SECONDS) {
			throw error("PDP signed_metadata exp claim is in the past, so the JWT has expired", args("exp", exp, "now", now));
		}

		logSuccess("PDP signed_metadata exp claim is valid", args("exp", exp));
		return env;
	}
}
