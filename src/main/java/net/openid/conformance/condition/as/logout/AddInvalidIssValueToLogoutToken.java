package net.openid.conformance.condition.as.logout;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddInvalidIssValueToLogoutToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = "logout_token_claims")
	@PostEnvironment(required = "logout_token_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("logout_token_claims");

		String iss = env.getString("logout_token_claims", "iss");

		//Append "INVALID" to iss
		String concat = (iss + "INVALID");

		claims.addProperty("iss", concat);

		env.putObject("logout_token_claims", claims);

		log("Added invalid iss to logout token claims", args("logout_token_claims", claims, "iss", concat));

		return env;

	}

}
