package io.fintechlabs.testframework.condition.as;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

import java.time.Instant;
import java.util.Date;

public class ValidateRequestObjectExp extends AbstractCondition {

	private int timeSkewMillis = 5 * 60 * 1000; // 5 minute allowable skew for testing
	private long oneDayMillis = 60 * 60 * 24 * 1000L; // Duration for one day

	@Override
	@PreEnvironment(required = {"authorization_request_object", "client"})
	public Environment evaluate(Environment env) {

		Instant now = Instant.now(); // to check timestamps

		Long exp = env.getLong("authorization_request_object", "claims.exp");

		if (exp == null) {
			throw error ("Missing exp");
		} else {
			if (now.minusMillis(timeSkewMillis).isAfter(Instant.ofEpochSecond(exp))) {
				throw error("Token expired", args("expiration", new Date(exp * 1000L), "now", now));
			}

			if (now.plusMillis(oneDayMillis).isBefore(Instant.ofEpochSecond(exp))) {
				throw error("Assertion expires unreasonable far in the future", args("expired-at", new Date(exp * 1000L), "now", now));
				//Arbitrary, allow for 1 day in the future, adhering to rest of code.
			}
		}

		logSuccess("Request object contains expiry time ", args("exp", new Date(exp * 1000L)));
		return env;

	}

}
