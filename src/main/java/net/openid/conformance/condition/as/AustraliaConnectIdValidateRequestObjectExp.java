package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.util.Date;

public class AustraliaConnectIdValidateRequestObjectExp extends AbstractCondition {

	private int timeSkewMillis = 2 * 60 * 1000; // 2 minute allowable skew for testing
	private long tenMinutesMillis = 10 * 60 * 1000L;

	@Override
	@PreEnvironment(required = "authorization_request_object" )
	public Environment evaluate(Environment env) {

		Instant now = Instant.now(); // to check timestamps

		Long exp = env.getLong("authorization_request_object", "claims.exp");

		if (exp == null) {
			throw error ("Missing exp, request object does not contain an 'exp' claim");
		} else {
			if (now.minusMillis(timeSkewMillis).isAfter(Instant.ofEpochSecond(exp))) {
				throw error("Request objec expired", args("exp", new Date(exp * 1000L), "now", now));
			}

			if (now.plusMillis(tenMinutesMillis).isBefore(Instant.ofEpochSecond(exp))) {
				throw error("Request object expires unreasonably far in the future", args("exp", new Date(exp * 1000L), "now", now));
			}
		}

		logSuccess("Request object contains a valid exp claim, expiry time", args("exp", new Date(exp * 1000L)));
		return env;

	}

}
