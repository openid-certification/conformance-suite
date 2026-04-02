package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class ValidateCredentialJWTExp extends AbstractCondition {

	private static final int timeSkewMillis = 5 * 60 * 1000; // 5 minute allowable skew for testing

	@Override
	@PreEnvironment(required = { "sdjwt" })
	public Environment evaluate(Environment env) {

		Instant now = Instant.now();

		Long exp = env.getLong("sdjwt", "credential.claims.exp");
		if (exp == null) {
			log("'exp' is not present");
			return env;
		}
		if (now.minusMillis(timeSkewMillis).isAfter(Instant.ofEpochSecond(exp))) {
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
