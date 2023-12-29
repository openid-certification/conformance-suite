package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class FAPIBrazilValidateIdTokenExp extends AbstractCondition {

	private int timeSkewMillis = 5 * 60 * 1000; // 5 minute allowable skew for testing

	@Override
	@PreEnvironment(required = { "id_token" } )
	public Environment evaluate(Environment env) {

		Long exp = env.getLong("id_token", "claims.exp");
		if (exp == null) {
			throw error("'exp' claim missing");
		}

		Instant expInstant = Instant.ofEpochSecond(exp);
		Instant _180daysFromNow = Instant.now().plus(Duration.ofDays(180)).minusMillis(timeSkewMillis);
		if (expInstant.isBefore(_180daysFromNow)) {
			throw error("Token exp has insufficient validity. At least 180 days validity is required.", args("exp", exp, "exp_in_utc", DateTimeFormatter.ISO_INSTANT.format(expInstant)));
		}

		logSuccess("ID token exp passed validation checks", args("exp", exp, "exp_in_utc", DateTimeFormatter.ISO_INSTANT.format(expInstant)));
		return env;

	}

}
