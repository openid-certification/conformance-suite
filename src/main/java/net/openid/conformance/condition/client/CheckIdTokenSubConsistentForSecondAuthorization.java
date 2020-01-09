package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Check sub doesn't change for a prompt=none request
 *
 * Equivalent of (part of) https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#same_authn
 */
public class CheckIdTokenSubConsistentForSecondAuthorization extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "first_id_token", "id_token" } )
	public Environment evaluate(Environment env) {
		JsonObject firstIdToken = env.getObject("first_id_token").getAsJsonObject("claims");
		JsonObject secondIdToken = env.getObject("id_token").getAsJsonObject("claims");

		String subFirst = env.getString("first_id_token", "claims.sub");

		String subSecond = env.getString("id_token", "claims.sub");

		if (!subFirst.equals(subSecond)) {
			throw error("The id_token from the first and second authorization contain different sub claims, but must contain the same sub.",
				args("first_id_token", firstIdToken, "second_id_token", secondIdToken));
		}

		logSuccess("sub is the same in the second id_token", args("first_id_token", firstIdToken, "second_id_token", secondIdToken));

		return env;
	}

}
