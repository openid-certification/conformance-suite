package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.util.Date;

public class ValidateRequestObjectIat extends AbstractCondition {

	private int timeSkewMillis = 5 * 60 * 1000; // 5 minute allowable skew for testing

	@Override
	@PreEnvironment(required = {"authorization_request_object", "client"})
	public Environment evaluate(Environment env) {
		Instant now = Instant.now();
		Long iat = env.getLong("authorization_request_object", "claims.iat");
		if (iat == null) {
			log(args("msg", "Request object does not contain an 'iat' claim", "result", ConditionResult.INFO));
		} else {
			if (now.plusMillis(timeSkewMillis).isBefore(Instant.ofEpochSecond(iat))) {
				throw error("Token issued in the future, 'iat' claim value is in the future",
					args("issued-at", new Date(iat * 1000L), "now", now));
			}
		}
		logSuccess("iat claim is valid", args("iat", iat));
		return env;
	}

}
