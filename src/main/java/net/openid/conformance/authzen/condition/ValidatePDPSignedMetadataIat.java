package net.openid.conformance.authzen.condition;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidatePDPSignedMetadataIat extends AbstractValidatePDPSignedMetadataTimeClaim {

	@Override
	@PreEnvironment(required = {"pdp_signed_metadata"})
	public Environment evaluate(Environment env) {
		Long iat = getTimeClaim(env, "iat");
		if (iat == null) {
			log("PDP signed_metadata has no iat claim (optional), skipping check");
			return env;
		}

		long now = nowSeconds();
		if (iat > now + CLOCK_SKEW_SECONDS) {
			throw error("PDP signed_metadata iat claim is in the future", args("iat", iat, "now", now));
		}

		logSuccess("PDP signed_metadata iat claim is valid", args("iat", iat));
		return env;
	}
}
