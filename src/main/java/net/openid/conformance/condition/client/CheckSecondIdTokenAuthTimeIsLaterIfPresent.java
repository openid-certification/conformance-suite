package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Check auth_time changes when the user re-logins in due to prompt=login in the request
 *
 * Equivalent of https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#multiple_sign_on - but this version
 * checks that auth_time in the second id_token is newer, instead of just different as the python does.
 */
public class CheckSecondIdTokenAuthTimeIsLaterIfPresent extends AbstractCondition {
	private static final String CLAIM_AUTH_TIME = "auth_time";

	@Override
	@PreEnvironment(required = { "first_id_token", "id_token" } )
	public Environment evaluate(Environment env) {
		JsonObject firstIdToken = env.getObject("first_id_token").getAsJsonObject("claims");
		JsonObject secondIdToken = env.getObject("id_token").getAsJsonObject("claims");

		if (firstIdToken.has(CLAIM_AUTH_TIME) && secondIdToken.has(CLAIM_AUTH_TIME)) {
			var firstAuthTime = OIDFJSON.getLong(firstIdToken.get(CLAIM_AUTH_TIME));
			var secondAuthTime = OIDFJSON.getLong(secondIdToken.get(CLAIM_AUTH_TIME));

			if (firstAuthTime == secondAuthTime) {
				throw error("prompt=login means the server was required to reauthenticate the user, the id_token from the second authorization incorrectly has the same auth_time as the id_token from the first authorization", args("first_id_token", firstIdToken, "second_id_token", secondIdToken));
			}

			if (firstAuthTime > secondAuthTime) {
				throw error("The id_token from the second authorization incorrectly has an earlier auth_time than the id_token from the first authorization", args("first_id_token", firstIdToken, "second_id_token", secondIdToken));
			}

			logSuccess("auth_time is later in the second id_token", args("first_id_token", firstIdToken, "second_id_token", secondIdToken));

		} else {
			log("auth_time cannot be checked as it is missing from the id_tokens for at least one of the authorizations", args("first_id_token", firstIdToken, "second_id_token", secondIdToken));
		}

		return env;
	}

}
