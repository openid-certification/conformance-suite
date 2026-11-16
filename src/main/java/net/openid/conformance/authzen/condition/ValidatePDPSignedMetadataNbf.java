package net.openid.conformance.authzen.condition;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidatePDPSignedMetadataNbf extends AbstractValidatePDPSignedMetadataTimeClaim {

	@Override
	@PreEnvironment(required = {"pdp_signed_metadata"})
	public Environment evaluate(Environment env) {
		Long nbf = getTimeClaim(env, "nbf");
		if (nbf == null) {
			log("PDP signed_metadata has no nbf claim (optional), skipping check");
			return env;
		}

		long now = nowSeconds();
		if (nbf > now + CLOCK_SKEW_SECONDS) {
			throw error("PDP signed_metadata nbf claim is in the future, so the JWT is not yet valid", args("nbf", nbf, "now", now));
		}

		logSuccess("PDP signed_metadata nbf claim is valid", args("nbf", nbf));
		return env;
	}
}
