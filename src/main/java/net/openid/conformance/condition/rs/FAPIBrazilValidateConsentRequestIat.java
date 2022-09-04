package net.openid.conformance.condition.rs;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.util.Date;

public class FAPIBrazilValidateConsentRequestIat extends AbstractCondition {
	private int timeSkewMillis = 5 * 60 * 1000; // 5 minute allowable skew for testing

	@Override
	@PreEnvironment(required = {"new_consent_request"})
	public Environment evaluate(Environment env) {
		Instant now = Instant.now(); // to check timestamps
		Long iat = env.getLong("new_consent_request", "claims.iat");
		if (iat != null) {
			if (now.plusMillis(timeSkewMillis).isBefore(Instant.ofEpochSecond(iat))) {
				throw error("Consent request iat is in the future", args("issued-at", new Date(iat * 1000L), "now", now));
			}
		}
		logSuccess("iat claim in consent request is valid", args("iat", iat));

		return env;
	}

}
