package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckIdTokenSidMatchesFrontChannelLogoutRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "id_token", "frontchannel_logout_request" } )
	public Environment evaluate(Environment env) {
		JsonObject idToken = env.getObject("id_token").getAsJsonObject("claims");
		JsonObject logoutRequest = env.getObject("frontchannel_logout_request").getAsJsonObject("query_string_params");

		String sidIdToken = env.getString("id_token", "claims.sid");

		String sidFrontChannel = env.getString("frontchannel_logout_request", "query_string_params.sid");

		if (Strings.isNullOrEmpty(sidFrontChannel)) {
			throw error("'sid' missing from frontchannel logout request");
		}

		if (!sidIdToken.equals(sidFrontChannel)) {
			throw error("The id_token and the frontchannel logout request contain different sid claims, but must contain the same sid.",
				args("id_token", idToken, "logout_request", logoutRequest));
		}

		logSuccess("sid is the same in the id_token and the front channel logout request", args("id_token", idToken, "logout_request", logoutRequest));

		return env;
	}

}
