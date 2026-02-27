package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * exp is optional, use with skipIfElementMissing
 */
public class OIDCCValidateRequestObjectExp extends AbstractCondition {

	private int timeSkewMillis = 5 * 60 * 1000; // 5 minute allowable skew for testing

	@Override
	@PreEnvironment(required = "authorization_request_object" )
	public Environment evaluate(Environment env) {

		Instant now = Instant.now();

		Long exp = env.getLong("authorization_request_object", "claims.exp");

		if (exp == null) {
			throw error ("Missing exp, request object does not contain an 'exp' claim");
		}

		if (now.minusMillis(timeSkewMillis).isAfter(Instant.ofEpochSecond(exp))) {
			throw error("Request object expired", args("exp", new Date(exp * 1000L), "now", now));
		}
		if (Instant.ofEpochSecond(exp).isAfter(now.plus(50 * 365, ChronoUnit.DAYS))) {
			throw error("'exp' is unreasonably far in the future (more than 50 years), this may indicate the value was incorrectly specified in milliseconds instead of seconds",
				args("exp", new Date(exp * 1000L), "now", now));
		}
		logSuccess("Request object contains a valid exp claim, expiry time", args("exp", new Date(exp * 1000L)));
		return env;
	}

}
