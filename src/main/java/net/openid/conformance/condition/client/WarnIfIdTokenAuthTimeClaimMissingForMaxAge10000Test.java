package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class WarnIfIdTokenAuthTimeClaimMissingForMaxAge10000Test extends AbstractCondition {
	private static final String CLAIM_AUTH_TIME = "auth_time";

	@Override
	@PreEnvironment(required = { "id_token" } )
	public Environment evaluate(Environment env) {
		JsonObject idToken = env.getObject("id_token").getAsJsonObject("claims");

		if (!idToken.has(CLAIM_AUTH_TIME)) {
			throw error("auth_time claim is missing from the id_token. This is permitted, but means the suite can't validate that the authorization_server skipped authorization when max_age=10000 is passed.", args("id_token", idToken));
		}

		logSuccess("auth_time is present in the id_token, so the suite can validate that the authorization_server skipped authorization when max_age=10000 is passed.", args("id_token", idToken));

		return env;
	}

}
