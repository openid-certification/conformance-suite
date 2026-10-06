package net.openid.conformance.authzen.condition;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Flags signed_metadata time claims that RFC 7519 permits but that suggest a value which
 * is not unix seconds, such as milliseconds or an epoch default: an iat or nbf before
 * 2012-01-01, when JWTs came into use, or an exp more than 50 years ahead. Also flags an iat in the future, as a JWT
 * cannot have been issued after it was received, and an exp that is not after the iat or the nbf, as such a JWT
 * was never valid: the exp and nbf checks each compare one claim with the current time, and allow for clock
 * skew, so they do not catch claims that contradict each other. Intended to be called at WARNING.
 */
public class CheckPDPSignedMetadataTimeClaimsArePlausible extends AbstractValidatePDPSignedMetadataTimeClaim {

	// 2012-01-01T00:00:00Z
	private static final long EARLIEST_PLAUSIBLE_SECONDS = 1325376000L;
	private static final long LATEST_PLAUSIBLE_EXP_YEARS = 50;
	private static final long SECONDS_PER_YEAR = 365L * 24 * 60 * 60;

	@Override
	@PreEnvironment(required = {"pdp_signed_metadata"})
	public Environment evaluate(Environment env) {
		for (String claim : new String[] {"iat", "nbf"}) {
			Long value = getTimeClaim(env, claim);
			if (value != null && value < EARLIEST_PLAUSIBLE_SECONDS) {
				throw error("PDP signed_metadata " + claim + " claim is before 2012-01-01, which may mean it is not a unix timestamp in seconds",
					args(claim, value));
			}
		}
		Long iat = getTimeClaim(env, "iat");
		long now = nowSeconds();
		if (iat != null && iat > now + CLOCK_SKEW_SECONDS) {
			throw error("PDP signed_metadata iat claim is in the future", args("iat", iat, "now", now));
		}
		Long exp = getTimeClaim(env, "exp");
		if (exp != null && exp > now + LATEST_PLAUSIBLE_EXP_YEARS * SECONDS_PER_YEAR) {
			throw error("PDP signed_metadata exp claim is more than " + LATEST_PLAUSIBLE_EXP_YEARS + " years in the future, which may mean it is not a unix timestamp in seconds",
				args("exp", exp));
		}
		if (exp != null) {
			for (String claim : new String[] {"iat", "nbf"}) {
				Long value = getTimeClaim(env, claim);
				if (value != null && exp <= value) {
					throw error("PDP signed_metadata exp claim is not after its " + claim + " claim, so the signed metadata was never valid",
						args("exp", exp, claim, value));
				}
			}
		}
		logSuccess("PDP signed_metadata time claims are plausible unix timestamps in seconds");
		return env;
	}
}
