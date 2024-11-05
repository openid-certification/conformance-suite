package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.time.Instant;

// Adds an iat/nbf/exp of 62 seconds in the future to the Client Authentication Assertion.
public class AddIatNbfExpOver60SecondsInTheFutureToClientAuthenticationAssertionClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"client_assertion_claims"})
	@PostEnvironment(required = "client_assertion_claims")
	public Environment evaluate(Environment env) {
		JsonObject claims = env.getObject("client_assertion_claims").getAsJsonObject();

		int offsetSeconds = 62;

		Instant iatTime = adjustTimestampIfClaimIsPresent(claims, "iat", offsetSeconds);
		Instant nbfTime = adjustTimestampIfClaimIsPresent(claims, "nbf", offsetSeconds);
		Instant expTime = adjustTimestampIfClaimIsPresent(claims, "exp", offsetSeconds);

		env.putObject("client_assertion_claims", claims);

		logSuccess("Added iat/nbf/exp values to client assertion claims which are 62 seconds in the future", args("client_assertion_claims", claims,
			"nbf_is_62_seconds_in_the_future", nbfTime, "iat_is_62_seconds_in_the_future", iatTime, "exp_is_62_seconds_in_the_future", expTime));

		return env;
	}

	private static Instant adjustTimestampIfClaimIsPresent(JsonObject claims, String timestampClaim, int offsetSeconds) {
		Instant time = null;
		if (claims.has(timestampClaim)) {
			long value = OIDFJSON.getLong(claims.get(timestampClaim));
			time = Instant.ofEpochSecond(value).plusSeconds(offsetSeconds);
			claims.addProperty(timestampClaim, time.getEpochSecond());
		}
		return time;
	}
}
