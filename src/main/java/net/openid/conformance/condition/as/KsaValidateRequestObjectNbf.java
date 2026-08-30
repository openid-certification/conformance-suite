package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;

/**
 * KSA Open Finance security profile: the request object must contain an nbf claim that is not
 * more than 10 minutes in the past.
 */
public class KsaValidateRequestObjectNbf extends AbstractCondition {

	private static final long TIME_SKEW_MILLIS = 2 * 60 * 1000L; // 2 minute allowable skew for testing
	private static final long TEN_MINUTES_MILLIS = 10 * 60 * 1000L;

	@Override
	@PreEnvironment(required = {"authorization_request_object", "client"})
	public Environment evaluate(Environment env) {

		Instant now = Instant.now(); // to check timestamps

		Long nbf = env.getLong("authorization_request_object", "claims.nbf");
		if (nbf == null) {
			throw error("Missing nbf claim in request object");
		}

		Instant nbfInstant = Instant.ofEpochSecond(nbf);
		if (nbfInstant.isBefore(now.minusMillis(TEN_MINUTES_MILLIS))) {
			throw error("nbf claim is more than 10 minutes in the past", args("nbf", nbfInstant, "now", now));
		}

		if (nbfInstant.isAfter(now.plusMillis(TIME_SKEW_MILLIS))) {
			throw error("nbf claim is in the future", args("nbf", nbfInstant, "now", now, "time_skew", TIME_SKEW_MILLIS));
		}

		logSuccess("nbf claim is valid", args("nbf", nbfInstant, "now", now));
		return env;

	}

}
