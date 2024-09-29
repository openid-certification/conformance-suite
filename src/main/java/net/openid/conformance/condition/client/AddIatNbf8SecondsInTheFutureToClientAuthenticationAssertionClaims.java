package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;

// Adds an iat/nbf of 8 seconds in the future to the Client Authentication Assertion.
public class AddIatNbf8SecondsInTheFutureToClientAuthenticationAssertionClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "client_assertion_claims" })
	@PostEnvironment(required = "client_assertion_claims")
	public Environment evaluate(Environment env) {
		JsonObject claims = env.getObject("client_assertion_claims").getAsJsonObject();
		Instant time = Instant.now().plusSeconds(8);

		claims.addProperty("iat", time.getEpochSecond());
		claims.addProperty("nbf", time.getEpochSecond());

		env.putObject("client_assertion_claims", claims);

		logSuccess("Added iat/nbf values to client assertion claims which are 8 seconds in the future",args("client_assertion_claims", claims,
			"nbf_is_8_seconds_in_the_future", time,  "iat_is_8_seconds_in_the_future", time));

		return env;
	}
}
