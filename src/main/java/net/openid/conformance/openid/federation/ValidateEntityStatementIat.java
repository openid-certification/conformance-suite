package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.util.Date;

public class ValidateEntityStatementIat extends AbstractCondition {

	private int timeSkewMillis = 5 * 60 * 1000; // 5 minute allowable skew for testing

	@Override
	@PostEnvironment(required = { "server", "config" } )
	public Environment evaluate(Environment env) {
		Instant now = Instant.now();
		Long iat = env.getLong("server", "iat");
		if (iat == null) {
			throw error("Entity statement does not contain an 'iat' claim");
		} else {
			if (now.plusMillis(timeSkewMillis).isBefore(Instant.ofEpochSecond(iat))) {
				throw error("Entity statement issued in the future, 'iat' claim value is in the future",
					args("issued-at", new Date(iat * 1000L), "now", now));
			}
		}
		logSuccess("iat claim is valid", args("iat", iat));
		return env;	}
}
