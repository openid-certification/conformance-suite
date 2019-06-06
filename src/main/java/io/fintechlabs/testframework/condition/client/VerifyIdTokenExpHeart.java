package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

import java.time.Instant;
import java.util.Date;

public class VerifyIdTokenExpHeart extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token")
	public Environment evaluate(Environment env) {
		Long exp = env.getLong("id_token", "claims.exp");
		Long iat = env.getLong("id_token", "claims.iat");
		if (iat == null) {
			throw error("Missing issuance time");
		} else if (exp == null) {
			throw error("Missing expiration");
		} else {
			if (Instant.ofEpochSecond(iat).plusSeconds(60 * 5).isBefore(Instant.ofEpochSecond(exp)) ) {
				throw error("Valid for more than 5 minutes", args("expiration", new Date(exp * 1000L), "issued_at", new Date(iat * 1000L)));
			}
			if (Instant.ofEpochSecond(iat).isAfter(Instant.ofEpochSecond(exp))) {
				throw error("Expired before issued", args("expiration", new Date(exp * 1000L), "issued_at", new Date(iat * 1000L)));
			}
		}
		return env;
	}
}
