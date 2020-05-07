package net.openid.conformance.condition.as.logout;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddSidToIdTokenClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"id_token_claims", "session_state_data"})
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("id_token_claims");

		String sid = env.getString("session_state_data", "sid");

		claims.addProperty("sid", sid);

		env.putObject("id_token_claims", claims);

		logSuccess("Added sid to ID token claims", args("id_token_claims", claims, "sid", sid));

		return env;

	}

}
