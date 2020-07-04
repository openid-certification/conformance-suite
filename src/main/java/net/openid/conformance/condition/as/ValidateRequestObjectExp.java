package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.util.Date;

public class ValidateRequestObjectExp extends AbstractCondition {

	private int timeSkewMillis = 5 * 60 * 1000; // 5 minute allowable skew for testing
	private long oneDayMillis = 60 * 60 * 24 * 1000L; // Duration for one day

	@Override
	@PreEnvironment(required = "authorization_request_object" )
	public Environment evaluate(Environment env) {

		Instant now = Instant.now(); // to check timestamps

		Long exp = env.getLong("authorization_request_object", "claims.exp");

		if (exp == null) {
			throw error ("Missing exp, request object does not contain an 'exp' claim");
		} else {
			if (now.minusMillis(timeSkewMillis).isAfter(Instant.ofEpochSecond(exp))) {
				throw error("Token expired", args("exp", new Date(exp * 1000L), "now", now));
			}

			if (now.plusMillis(oneDayMillis).isBefore(Instant.ofEpochSecond(exp))) {
				throw error("Assertion expires unreasonably far in the future", args("exp", new Date(exp * 1000L), "now", now));
				//Arbitrary, allow for 1 day in the future, adhering to rest of code.
			}
		}

		logSuccess("Request object contains a valid exp claim, expiry time", args("exp", new Date(exp * 1000L)));
		return env;

	}

}
