package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.util.Date;

/**
 * KSA Open Finance security profile: the request object must contain an exp claim that has a
 * lifetime of no longer than 10 minutes after the nbf claim.
 */
public class KsaValidateRequestObjectExp extends AbstractCondition {

	private static final long TIME_SKEW_MILLIS = 2 * 60 * 1000L; // 2 minute allowable skew for testing
	private static final long TEN_MINUTES_SECONDS = 10 * 60L;

	@Override
	@PreEnvironment(required = "authorization_request_object")
	public Environment evaluate(Environment env) {

		Instant now = Instant.now(); // to check timestamps

		Long exp = env.getLong("authorization_request_object", "claims.exp");
		if (exp == null) {
			throw error("Missing exp, request object does not contain an 'exp' claim");
		}

		Long nbf = env.getLong("authorization_request_object", "claims.nbf");
		if (nbf == null) {
			throw error("Missing nbf, the request object 'exp' lifetime is defined relative to 'nbf'");
		}

		if (now.minusMillis(TIME_SKEW_MILLIS).isAfter(Instant.ofEpochSecond(exp))) {
			throw error("Request object expired", args("exp", new Date(exp * 1000L), "now", now));
		}

		long lifetime = exp - nbf;
		if (lifetime > TEN_MINUTES_SECONDS) {
			throw error("Request object 'exp' is more than 10 minutes after 'nbf'",
				args("exp", new Date(exp * 1000L), "nbf", new Date(nbf * 1000L), "lifetime_seconds", lifetime));
		}

		logSuccess("Request object 'exp' is no more than 10 minutes after 'nbf'",
			args("exp", new Date(exp * 1000L), "nbf", new Date(nbf * 1000L), "lifetime_seconds", lifetime));
		return env;

	}

}
