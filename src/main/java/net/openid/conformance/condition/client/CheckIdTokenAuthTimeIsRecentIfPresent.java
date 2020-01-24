package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.time.Instant;
import java.util.Date;

/**
 * Equivalent of https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#auth_time_check
 */
public class CheckIdTokenAuthTimeIsRecentIfPresent extends AbstractCondition {
	private static final String CLAIM_AUTH_TIME = "auth_time";

	private int timeSkewMillis = 5 * 60 * 1000; // 5 minute allowable skew for testing

	@Override
	@PreEnvironment(required = "id_token" )
	public Environment evaluate(Environment env) {
		JsonObject idToken = env.getObject("id_token").getAsJsonObject("claims");

		if (idToken.has(CLAIM_AUTH_TIME)) {
			var authTime = OIDFJSON.getLong(idToken.get(CLAIM_AUTH_TIME));

			if (Instant.now().minusMillis(timeSkewMillis).minusSeconds(1).isAfter(Instant.ofEpochSecond(authTime))) {
				throw error("id_token auth_time is older than 1 second (allowing 5 minutes skews)", args("auth_time", new Date(authTime * 1000L), "now", Instant.now()));
			}

			// ValidateIdToken already checked if auth_time is in the future

			logSuccess("auth_time in id_token is recent", args("auth_time", new Date(authTime * 1000L), "now", Instant.now()));
		} else {
			log("auth_time cannot be checked as it is missing from the id_token", args("id_token", idToken));
		}

		return env;
	}

}
