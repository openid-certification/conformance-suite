package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Check auth_time doesn't change for a prompt=none request
 *
 * Equivalent of (part of) https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#same_authn
 */
public class CheckIdTokenAuthTimeClaimsSameIfPresent extends AbstractCondition {
	private static final String CLAIM_AUTH_TIME = "auth_time";

	@Override
	@PreEnvironment(required = { "first_id_token", "id_token" } )
	public Environment evaluate(Environment env) {
		JsonObject firstIdToken = env.getObject("first_id_token").getAsJsonObject("claims");
		JsonObject secondIdToken = env.getObject("id_token").getAsJsonObject("claims");

		if (firstIdToken.has(CLAIM_AUTH_TIME) && secondIdToken.has(CLAIM_AUTH_TIME)) {
			var firstAuthTime = OIDFJSON.getLong(firstIdToken.get(CLAIM_AUTH_TIME));
			var authTime = OIDFJSON.getLong(secondIdToken.get(CLAIM_AUTH_TIME));

			if (firstAuthTime != authTime) {
				throw error("The id_tokens contain different auth_time claims, but must contain the same auth_time.", args("first_id_token", firstIdToken, "second_id_token", secondIdToken));
			}

			logSuccess("auth_time is the same in the second id_token", args("first_id_token", firstIdToken, "second_id_token", secondIdToken));

		} else {
			log("auth_time cannot be checked as it is missing from the id_tokens for at least one of the authorizations", args("first_id_token", firstIdToken, "second_id_token", secondIdToken));
		}

		return env;
	}

}
