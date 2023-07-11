package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.util.Date;

public class ValidateCredentialJWTIat extends AbstractCondition {
	/** A relatively arbitary choice, but given the SDJWT spec was only created in 2022 no way a token could have
	 * an iat of 2019 or before */
	final Long secondsSince1Jan2019 = 1546300800L;

	private int timeSkewMillis = 5 * 60 * 1000; // 5 minute allowable skew for testing

	@Override
	@PreEnvironment(required = { "sdjwt" } )
	public Environment evaluate(Environment env) {
		Instant now = Instant.now(); // to check timestamps

		Long iat = env.getLong("sdjwt", "credential.claims.iat");

		// As per https://www.ietf.org/id/draft-ietf-oauth-sd-jwt-vc-00.html#section-4.2.2.2
		if (iat == null) {
			throw error("iat missing from credential jwt");
		}

		if (now.plusMillis(timeSkewMillis).isBefore(Instant.ofEpochSecond(iat))) {
			throw error("credential jwt 'iat' is in the future", args("issued-at", new Date(iat * 1000L), "now", now));
		}
		if (iat < secondsSince1Jan2019) {
			throw error("credential jwt 'iat' is before 1st Jan 2019", args("issued-at", new Date(iat * 1000L), "1stJan2019", secondsSince1Jan2019));
		}

		logSuccess("'iat' has a sensible value");

		return env;
	}

}
