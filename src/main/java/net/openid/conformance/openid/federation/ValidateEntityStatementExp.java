package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class ValidateEntityStatementExp extends AbstractCondition {

	// 5 minute allowable skew for testing. The number 5 is arbitrary and not mentioned in the specification.
	private final int timeSkewMillis = 5 * 60 * 1000;

	@Override
	public Environment evaluate(Environment env) {
		Instant now = Instant.now();

		Long exp = env.getLong("federation_response_exp");

		if (now.minusMillis(timeSkewMillis).isAfter(Instant.ofEpochSecond(exp))) {
			throw error("Entity statement expired", args("exp", new Date(exp * 1000L), "now", now));
		}
		if (now.plus(5 * 365, ChronoUnit.DAYS).isBefore(Instant.ofEpochSecond(exp))) {
			throw error("Entity statement is set to expire more than five years in the future", args("exp", new Date(exp * 1000L)));
		}
		logSuccess("Entity statement contains a valid exp claim, expiry time", args("exp", new Date(exp * 1000L)));
		return env;
	}
}
