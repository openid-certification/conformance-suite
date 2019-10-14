package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;

public class AddExpIs5MinutesInPastToClientAssertionClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client_assertion_claims")
	@PostEnvironment(required = "client_assertion_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("client_assertion_claims");

		Instant exp = Instant.now().minusSeconds(5*60);

		claims.addProperty("exp", exp.getEpochSecond());

		env.putObject("client_assertion_claims", claims);

		logSuccess("Added 'exp' is 5 minutes in the past to client_assertion_claims", claims);

		return env;
	}
}
