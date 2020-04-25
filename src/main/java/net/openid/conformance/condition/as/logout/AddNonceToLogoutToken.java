package net.openid.conformance.condition.as.logout;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddNonceToLogoutToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = "logout_token_claims")
	@PostEnvironment(required = "logout_token_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("logout_token_claims");


		claims.addProperty("nonce", "logout_tokens_must_not_contain_a_nonce");

		env.putObject("logout_token_claims", claims);

		log("Added nonce claim to logout token claims", args("logout_token_claims", claims));

		return env;

	}

}
