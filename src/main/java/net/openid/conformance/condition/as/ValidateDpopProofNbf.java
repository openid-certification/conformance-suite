package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.util.Date;

public class ValidateDpopProofNbf extends AbstractCondition {

	// TODO: make this configurable
	private int timeSkewMillis = AbstractValidateDpopProof.timeSkewMillis; // 5 minute allowable skew for testing

	@Override
	@PreEnvironment(required = {"incoming_dpop_proof"})
	public Environment evaluate(Environment env) {
		Instant now = Instant.now(); // to check timestamps

		Long nbf = env.getLong("incoming_dpop_proof", "claims.nbf");
		if (nbf != null) {
			if (now.plusMillis(timeSkewMillis).isBefore(Instant.ofEpochSecond(nbf))) {
				throw error("DPoP Proof has future not-before", args("not-before", new Date(nbf * 1000L), "now", now));
			}
		}

		logSuccess("DPoP Proof nbf value passed validation checks");
		return env;
	}


}
