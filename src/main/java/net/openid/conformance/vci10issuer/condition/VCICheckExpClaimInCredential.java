package net.openid.conformance.vci10issuer.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class VCICheckExpClaimInCredential extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "sdjwt" } )
	public Environment evaluate(Environment env) {

		Instant now = Instant.now(); // to check timestamps

		Long exp = env.getLong("sdjwt", "credential.claims.exp");
		if (exp == null) {
			logSuccess("No 'exp' claim to check");
			return env;
		}
		if (now.isAfter(Instant.ofEpochSecond(exp))) {
			throw error("Credential 'exp' has expired", args("expires-at", new Date(exp * 1000L), "now", now));
		}
		if (Instant.ofEpochSecond(exp).isAfter(now.plus(50 * 365, ChronoUnit.DAYS))) {
			throw error("'exp' is unreasonably far in the future (more than 50 years), this may indicate the value was incorrectly specified in milliseconds instead of seconds",
				args("exp", new Date(exp * 1000L), "now", now));
		}

		logSuccess("Credential 'exp' passed validation checks");
		return env;

	}
}
