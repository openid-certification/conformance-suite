package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.util.Date;

/**
 * https://openid.net/specs/openid-connect-core-1_0.html#RequestUriParameter
 * ...Servers MAY cache the contents of the resources referenced by Request URIs....
 * Therefore this class does NOT require 'exp' to have a max 1 day lifetime as
 * ValidateRequestObjectExp
 */
public class ValidateRequestObjectExpForRequestUriClientRequestType extends AbstractCondition {

	private int timeSkewMillis = 5 * 60 * 1000; // 5 minute allowable skew for testing

	@Override
	@PreEnvironment(required = "authorization_request_object" )
	public Environment evaluate(Environment env) {

		Instant now = Instant.now();

		Long exp = env.getLong("authorization_request_object", "claims.exp");

		if (exp == null) {
			throw error ("Missing exp, request object does not contain an 'exp' claim");
		} else {
			if (now.minusMillis(timeSkewMillis).isAfter(Instant.ofEpochSecond(exp))) {
				throw error("Token expired", args("exp", new Date(exp * 1000L), "now", now));
			}
		}
		logSuccess("Request object contains a valid exp claim, expiry time", args("exp", new Date(exp * 1000L)));
		return env;
	}

}
