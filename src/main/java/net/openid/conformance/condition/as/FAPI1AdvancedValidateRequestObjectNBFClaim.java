package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;

/**
 * 5.2.2-17 Must reject request objects where the nbf claim in request object is missing or is more than 60 minutes in the past.
 */
public class FAPI1AdvancedValidateRequestObjectNBFClaim extends AbstractCondition {

	private int TIME_SKEW = 5 * 60 * 1000; // 5 minute allowable skew for testing
	private int SIXTY_MINUTES = 60 * 60 * 1000;

	@Override
	@PreEnvironment(required = {"authorization_request_object", "client"})
	public Environment evaluate(Environment env) {

		Instant now = Instant.now(); // to check timestamps

		Long nbf = env.getLong("authorization_request_object", "claims.nbf");
		if (nbf == null) {
			throw error("Missing nbf claim in request object");
		}
		Instant nbfInstant = Instant.ofEpochSecond(nbf);
		if (nbfInstant.isBefore(now.minusMillis(SIXTY_MINUTES))) {
			throw error("nbf claim is more than 60 minutes in the past", args("nbf", nbfInstant, "now", now));
		}

		if(nbfInstant.isAfter(now.plusMillis(TIME_SKEW))) {
			throw error("nbf claim is in the future", args("nbf", nbfInstant, "now", now, "time_skew", TIME_SKEW));
		}

		logSuccess("nbf claim is valid", args("nbf", nbfInstant, "now", now));
		return env;


	}

}
