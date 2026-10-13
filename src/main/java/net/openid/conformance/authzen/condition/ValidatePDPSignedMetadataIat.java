package net.openid.conformance.authzen.condition;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * RFC 7519 Section 4.1.6: the iat claim's "value MUST be a number containing a NumericDate
 * value". {@link ExtractPDPSignedMetadata} has already rejected one that is not a number, so
 * this records the claim. It has no validity rule, so an implausible value (such as one in the
 * future) is left to {@link CheckPDPSignedMetadataTimeClaimsArePlausible}.
 */
public class ValidatePDPSignedMetadataIat extends AbstractValidatePDPSignedMetadataTimeClaim {

	@Override
	@PreEnvironment(required = {"pdp_signed_metadata"})
	public Environment evaluate(Environment env) {
		Long iat = getTimeClaim(env, "iat");
		if (iat == null) {
			log("PDP signed_metadata has no iat claim (optional), skipping check");
			return env;
		}

		logSuccess("PDP signed_metadata iat claim is a NumericDate", args("iat", iat));
		return env;
	}
}
