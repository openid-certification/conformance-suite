package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;

/**
 * 5.2.2-17 Must reject request objects where the nbf claim in request object is missing or is more than 60 minutes in the past.
 */
public class FAPI1AdvancedValidateBackchannelRequestObjectNBFClaim extends AbstractCondition {

	private int SIXTY_MINUTES = 60 * 60 * 1000;

	@Override
	@PreEnvironment(required = {"backchannel_request_object", "client"})
	public Environment evaluate(Environment env) {

		Instant now = Instant.now(); // to check timestamps

		Long nbf = env.getLong("backchannel_request_object", "claims.nbf");
		if (nbf == null) {
			throw error("Missing nbf claim in request object");
		}
		Instant nbfInstant = Instant.ofEpochSecond(nbf);
		if (nbfInstant.isBefore(now.minusMillis(SIXTY_MINUTES))) {
			throw error("nbf claim is more than 60 minutes in the past", args("nbf", nbfInstant, "now", now));
		}

		logSuccess("nbf claim is valid", args("nbf", nbfInstant, "now", now));
		return env;


	}

}
