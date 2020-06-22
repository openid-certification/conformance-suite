package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckIdTokenSubMatchesLogoutToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "id_token", "logout_token" } )
	public Environment evaluate(Environment env) {
		JsonObject idToken = env.getObject("id_token").getAsJsonObject("claims");
		JsonObject logoutToken = env.getObject("logout_token").getAsJsonObject("claims");

		String subIdToken = env.getString("id_token", "claims.sub");

		String subLogoutToken = env.getString("logout_token", "claims.sub");

		if (!subIdToken.equals(subLogoutToken)) {
			throw error("The id_token and the logout_token contain different sub claims, but must contain the same sub.",
				args("id_token", idToken, "logout_token", logoutToken));
		}

		logSuccess("sub from the id_token matches that in the logout_token", args("id_token", idToken, "logout_token", logoutToken));

		return env;
	}

}
