package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddWrongAudToClientAssertionClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client_assertion_claims")
	@PostEnvironment(required = "client_assertion_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("client_assertion_claims");

		claims.addProperty("aud", "https://fapidev-rs.authlete.net/api/userinfo");

		env.putObject("client_assertion_claims", claims);

		logSuccess("Added wrong aud to client_assertion_claims", claims);

		return env;
	}
}
