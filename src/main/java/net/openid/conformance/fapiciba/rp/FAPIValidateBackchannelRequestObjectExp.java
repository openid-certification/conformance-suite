package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.util.Date;

public class FAPIValidateBackchannelRequestObjectExp extends AbstractCondition {

	private int timeSkewMillis = 5 * 60 * 1000; // 5 minute allowable skew for testing
	private long sixtyMinutesMillis = 60 * 60 * 1000L;

	@Override
	@PreEnvironment(required = "backchannel_request_object" )
	public Environment evaluate(Environment env) {

		Instant now = Instant.now(); // to check timestamps

		Long exp = env.getLong("backchannel_request_object", "claims.exp");

		if (exp == null) {
			throw error ("Missing exp, request object does not contain an 'exp' claim");
		} else {
			if (now.minusMillis(timeSkewMillis).isAfter(Instant.ofEpochSecond(exp))) {
				throw error("Token expired", args("exp", new Date(exp * 1000L), "now", now));
			}

			if (now.plusMillis(sixtyMinutesMillis).isBefore(Instant.ofEpochSecond(exp))) {
				throw error("Request object expires unreasonably far in the future", args("exp", new Date(exp * 1000L), "now", now));
			}
		}

		logSuccess("Request object contains a valid exp claim, expiry time", args("exp", new Date(exp * 1000L)));
		return env;

	}

}
