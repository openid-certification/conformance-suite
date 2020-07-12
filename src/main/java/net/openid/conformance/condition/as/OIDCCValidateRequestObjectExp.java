package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
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

		if (now.minusMillis(timeSkewMillis).isAfter(Instant.ofEpochSecond(exp))) {
			throw error("Request object expired", args("exp", new Date(exp * 1000L), "now", now));
		}
		logSuccess("Request object contains a valid exp claim, expiry time", args("exp", new Date(exp * 1000L)));
		return env;
	}

}
