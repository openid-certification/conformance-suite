package io.fintechlabs.testframework.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;
import io.fintechlabs.testframework.testmodule.OIDFJSON;

import java.time.Instant;
import java.util.Date;

public class ValidateClientAssertionClaims extends AbstractCondition {

	// TODO: make this configurable
	private int timeSkewMillis = 5 * 60 * 1000; // 5 minute allowable skew for testing
	private long oneDayMillis = 60 * 60 * 24 * 1000L; // Duration for one day

	@Override
	@PreEnvironment(required = "token_endpoint_request")
	public Environment evaluate(Environment env) {


		Instant now = Instant.now(); // to check timestamps

		String clientId = env.getString("client", "client_id"); // to check the client
		String issuer = env.getString("server", "issuer"); // to validate the issuer
		String tokenEndpoint = env.getString("server", "token_endpoint"); // to validate the audience

		// check all our testable values
		if (Strings.isNullOrEmpty(clientId)
			|| Strings.isNullOrEmpty(issuer)
			|| Strings.isNullOrEmpty(tokenEndpoint)) {
			throw error("Couldn't find issuer or client or token endpoint values in the test configuration to test the assertion");
		}

		JsonElement iss = env.getElementFromObject("client_assertion", "claims.iss");
		if (iss == null) {
			throw error("Missing iss");
		}
		if (!clientId.equals(env.getString("client_assertion", "claims.iss"))) {
			throw error("Issuer mismatch", args("expected", clientId, "actual", env.getString("client_assertion", "claims.iss")));
		}

		JsonElement aud = env.getElementFromObject("client_assertion", "claims.aud");
		if (aud == null) {
			throw error("Missing aud");
		}

		if (aud.isJsonArray()) {
			if (!aud.getAsJsonArray().contains(new JsonPrimitive(tokenEndpoint))) {
				throw error("aud not found", args("expected", tokenEndpoint, "actual", aud));
			}
		} else {
			if (!tokenEndpoint.equals(OIDFJSON.getString(aud))) {
				throw error("aud mismatch", args("expected", tokenEndpoint, "actual", aud));
			}
		}

		JsonElement sub = env.getElementFromObject("client_assertion", "claims.sub");
		if (sub == null) {
			throw error("Missing sub");
		}

		JsonElement jti = env.getElementFromObject("client_assertion", "claims.jti");
		if (jti == null) {
			throw error("Missing JWT ID");
		}

		Long nbf = env.getLong("client_assertion", "claims.nbf");
		if (nbf != null) {
			if (now.plusMillis(timeSkewMillis).isBefore(Instant.ofEpochSecond(nbf))) {
				throw error("Assertion 'nbf' value is in the future'", args("not-before", new Date(nbf * 1000L), "now", now));
			}
		}

		Long exp = env.getLong("client_assertion", "claims.exp");
		if (exp == null) {
			throw error("Missing exp");
		} else {
			if (now.minusMillis(timeSkewMillis).isAfter(Instant.ofEpochSecond(exp))) {
				throw error("Assertion expired", args("expiration", new Date(exp * 1000L), "now", now));
			}
			if (now.plusMillis(oneDayMillis).isBefore(Instant.ofEpochSecond(exp))) {
				throw error("Assertion expires unreasonable far in the future", args("expired-at", new Date(exp * 1000L), "now", now));
				//Arbitrary, allow for 1 day in the future as standard says "unreasonably far".
			}
		}

		Long iat = env.getLong("client_assertion", "claims.iat");
		if (iat == null) {
			throw error("Missing iat");
		} else {
			if (now.plusMillis(oneDayMillis).isBefore(Instant.ofEpochSecond(iat))) {
				throw error("Assertion expires unreasonable far in the future", args("issued-at", new Date(exp * 1000L), "now", now));
				//Arbitrary, allow for 1 day in the future as standard says "unreasonably far".
			}
		}

		logSuccess("Client Assertion passed all validation checks");
		return env;
	}
}
