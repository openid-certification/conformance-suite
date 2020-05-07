package net.openid.conformance.condition.as.logout;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddInvalidAudValueToLogoutToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = "logout_token_claims")
	@PostEnvironment(required = "logout_token_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("logout_token_claims");

		String aud = env.getString("logout_token_claims", "aud");

		//Append "INVALID" to aud
		String concat = (aud + "INVALID");

		claims.addProperty("aud", concat);

		env.putObject("logout_token_claims", claims);

		log("Added invalid aud to logout token claims", args("logout_token_claims", claims, "aud", concat));

		return env;

	}

}
