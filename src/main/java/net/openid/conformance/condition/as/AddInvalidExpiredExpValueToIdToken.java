package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;

public class AddInvalidExpiredExpValueToIdToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token_claims")
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("id_token_claims");

		Instant exp = Instant.now().minusSeconds(60 * 6);

		claims.addProperty("exp", exp.getEpochSecond());

		env.putObject("id_token_claims", claims);

		logSuccess("Added expired exp value to ID token claims", args("id_token_claims", claims, "exp", exp));

		return env;

	}

}
