package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.util.Date;

public class ValidateDpopProofIat extends AbstractCondition {

	// TODO: make this configurable
	private int timeSkewMillis = AbstractValidateDpopProof.timeSkewMillis; // 5 minute allowable skew for testing

	@Override
	@PreEnvironment(required = {"incoming_dpop_proof"})
	public Environment evaluate(Environment env) {
		Instant now = Instant.now(); // to check timestamps

		Long iat = env.getLong("incoming_dpop_proof", "claims.iat");
		if (iat == null) {
			throw error("'iat' claim in DPoP Proof is missing");
		}

		if (now.plusMillis(timeSkewMillis).isBefore(Instant.ofEpochSecond(iat))) {
			throw error("DPoP Proof 'iat' is in the future", args("issued-at", new Date(iat * 1000L), "now", now));
		}
		if (now.minusMillis(timeSkewMillis).isAfter(Instant.ofEpochSecond(iat))) {
			// as per OIDCC, the client can reasonably assume servers send iat values that match the current time:
			// "The iat Claim can be used to reject tokens that were issued too far away from the current time, limiting
			// the amount of time that nonces need to be stored to prevent attacks. The acceptable range is Client specific."
			throw error("DPoP Proof  'iat' is more than 5 minutes in the past", args("issued-at", new Date(iat * 1000L), "now", now));
		}

		logSuccess("DPoP Proof iat value passed validation checks");
		return env;
	}


}
