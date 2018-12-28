package io.fintechlabs.testframework.condition.as;

import java.time.Instant;
import java.util.Date;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class ValidateResourceAssertionClaims extends AbstractCondition {

	// TODO: make this configurable
	private int timeSkewMillis = 5 * 60 * 1000; // 5 minute allowable skew for testing

	/**
	 * @param testId
	 * @param log
	 * @param conditionResultOnFailure
	 * @param requirements
	 */
	public ValidateResourceAssertionClaims(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = {"resource_assertion", "server"}, strings = {"resource_id", "issuer"})
	public Environment evaluate(Environment env) {

		JsonObject payload = env.getElementFromObject("resource_assertion", "assertion_payload").getAsJsonObject();

		if (payload == null) {
			throw error("Couldn't find assertion payload");
		}

		String resourceId = env.getString("resource_id");

		String iss = env.getString("resource_assertion", "assertion_payload.iss");

		if (!resourceId.equals(iss)) {
			throw error("Issuer didn't match resource ID", args("expected", resourceId, "actual", iss));
		}

		String issuer = env.getString("issuer");
		String introspectionEndpoint = env.getString("server", "introspection_endpoint");

		JsonElement a = env.getElementFromObject("resource_assertion", "assertion_payload.aud");

		if (a == null) {
			throw error("Couldn't find audience");
		}

		if (a.isJsonArray()) {
			JsonArray aud = a.getAsJsonArray();

			if (!aud.contains(new JsonPrimitive(issuer)) && !aud.contains(new JsonPrimitive(introspectionEndpoint))) {
				throw error("Audience didn't match the server issuer", args("issuer", issuer, "endpoint", introspectionEndpoint, "actual", aud));
			}
		} else {
			String aud = a.getAsString();

			if (!issuer.equals(aud) && !introspectionEndpoint.equals(aud)) {
				throw error("Audience didn't match the server issuer", args("issuer", issuer, "endpoint", introspectionEndpoint, "actual", aud));
			}
		}

		Instant now = Instant.now();

		Long exp = env.getLong("resource_assertion", "assertion_payload.exp");
		if (exp == null) {
			throw error("Missing expiration");
		} else {
			if (now.minusMillis(timeSkewMillis).isAfter(Instant.ofEpochSecond(exp))) {
				throw error("Assertion expired", args("expiration", new Date(exp * 1000L), "now", now));
			}
		}

		Long iat = env.getLong("resource_assertion", "assertion_payload.iat");
		if (iat == null) {
			throw error("Missing issuance time");
		} else {
			if (now.plusMillis(timeSkewMillis).isBefore(Instant.ofEpochSecond(iat))) {
				throw error("Assertion issued in the future", args("issued-at", new Date(iat * 1000L), "now", now));
			}
		}

		Long nbf = env.getLong("resource_assertion", "assertion_payload.nbf");
		if (nbf != null) {
			if (now.plusMillis(timeSkewMillis).isBefore(Instant.ofEpochSecond(nbf))) {
				// this is just something to log, it doesn't make the token invalid
				log("Assertion has future not-before", args("not-before", new Date(nbf * 1000L), "now", now));
			}
		}

		logSuccess("Assertion payload passes all checks", payload);

		return env;

	}

}
