package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.util.Date;

public class ValidateEntityStatementExp extends AbstractCondition {

	private int timeSkewMillis = 5 * 60 * 1000; // 5 minute allowable skew for testing

	@Override
	@PreEnvironment(required = { "entity_statement_body" } )
	public Environment evaluate(Environment env) {

		Instant now = Instant.now();

		Long exp = env.getLong("entity_statement_body", "exp");

		if (now.minusMillis(timeSkewMillis).isAfter(Instant.ofEpochSecond(exp))) {
			throw error("Entity statement expired", args("exp", new Date(exp * 1000L), "now", now));
		}
		logSuccess("Entity statement contains a valid exp claim, expiry time", args("exp", new Date(exp * 1000L)));
		return env;
	}
}
