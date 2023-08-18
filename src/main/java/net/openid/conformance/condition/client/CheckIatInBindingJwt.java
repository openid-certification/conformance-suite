package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.util.Date;

public class CheckIatInBindingJwt extends AbstractCondition {

	private int timeSkewMillis = 5 * 60 * 1000; // 5 minute allowable skew for testing

	@Override
	@PreEnvironment(required = { "sdjwt" } )
	public Environment evaluate(Environment env) {

		Instant now = Instant.now(); // to check timestamps

		Long iat = env.getLong("sdjwt", "binding.claims.iat");
		if (iat == null) {
			throw error("'iat' claim missing");
		}
		if (now.plusMillis(timeSkewMillis).isBefore(Instant.ofEpochSecond(iat))) {
			throw error("Token 'iat' in the future", args("issued-at", new Date(iat * 1000L), "now", now));
		}
		if (now.minusMillis(timeSkewMillis).isAfter(Instant.ofEpochSecond(iat))) {
			throw error("Token 'iat' more than 5 minutes in the past", args("issued-at", new Date(iat * 1000L), "now", now));
		}

		logSuccess("Binding JWT iat passed validation checks");
		return env;

	}

}
