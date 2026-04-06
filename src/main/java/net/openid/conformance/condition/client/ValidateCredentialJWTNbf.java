package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class ValidateCredentialJWTNbf extends AbstractCondition {

	/** A relatively arbitrary choice, but given the SD-JWT spec was only created in 2022 no way a token could have
	 * a nbf of 2019 or before */
	private static final long secondsSince1Jan2019 = 1546300800L;

	private static final int timeSkewMillis = 5 * 60 * 1000; // 5 minute allowable skew for testing

	@Override
	@PreEnvironment(required = { "sdjwt" })
	public Environment evaluate(Environment env) {

		Instant now = Instant.now();

		Long nbf = env.getLong("sdjwt", "credential.claims.nbf");
		if (nbf == null) {
			log("'nbf' is not present");
			return env;
		}

		if (Instant.ofEpochSecond(nbf).isAfter(now.plusMillis(timeSkewMillis))) {
			throw error("Credential 'nbf' is in the future, credential is not yet valid",
				args("not-before", new Date(nbf * 1000L), "now", now));
		}
		if (Instant.ofEpochSecond(nbf).isAfter(now.plus(50 * 365, ChronoUnit.DAYS))) {
			throw error("'nbf' is unreasonably far in the future (more than 50 years), this may indicate the value was incorrectly specified in milliseconds instead of seconds",
				args("nbf", new Date(nbf * 1000L), "now", now));
		}
		if (nbf < secondsSince1Jan2019) {
			throw error("Credential 'nbf' is before 1st Jan 2019",
				args("not-before", new Date(nbf * 1000L), "1stJan2019", secondsSince1Jan2019));
		}

		logSuccess("Credential 'nbf' passed validation checks");
		return env;
	}
}
