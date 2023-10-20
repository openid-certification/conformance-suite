package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class VerifyIdTokenValidityIsMinimum180Days extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token")
	public Environment evaluate(Environment env) {

		Long iat = env.getLong("id_token", "claims.iat");
		Long exp = env.getLong("id_token", "claims.exp");
		long tokenValidityInDays = ChronoUnit.DAYS.between(Instant.ofEpochSecond(iat), Instant.ofEpochSecond(exp));
		if (tokenValidityInDays < 180) {
			throw error("‘exp’ in all issued id_tokens is supposed to be at least 180 days from the time of issue",
				args("iat", iat, "exp", exp, "token_validity_in_days", tokenValidityInDays));
		}

		logSuccess("Token validity is sufficient", args("token_validity_in_days", tokenValidityInDays));

		return env;
	}

}
