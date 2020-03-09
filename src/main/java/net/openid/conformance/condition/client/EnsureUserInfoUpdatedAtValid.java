package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;

public class EnsureUserInfoUpdatedAtValid extends AbstractCondition {
	private int timeSkewMillis = 5 * 60 * 1000; // 5 minute allowable skew for testing

	@Override
	@PreEnvironment(required = "userinfo")
	public Environment evaluate(Environment env) {
		Instant now = Instant.now();
		Long updatedAt = env.getLong("userinfo", "updated_at");

		// a relatively arbitrary choice, but if the updated_at data is prior to 1990 then it seems impossible that
		// it's valid as it would predate 'the web'.
		final Instant instantAtJan1990 = LocalDate.parse("1990-01-01").atStartOfDay().toInstant(ZoneOffset.UTC);
		if (updatedAt == null) {
			log("userinfo response does not contain 'updated_at'");
			return env;
		}

		if (now.plusMillis(timeSkewMillis).isBefore(Instant.ofEpochSecond(updatedAt))) {
			throw error("updated_at appears to be in the future", args("updated_at", new Date(updatedAt * 1000L), "now", now));
		}
		if (instantAtJan1990.isAfter(Instant.ofEpochSecond(updatedAt))) {
			throw error("updated_at appears to be prior to the year 1990", args("updated_at", new Date(updatedAt * 1000L), "now", now));
		}

		logSuccess("'updated_at' in userinfo response seems to be a valid time", args("updated_at", new Date(updatedAt * 1000L), "now", now));

		return env;
	}

}
