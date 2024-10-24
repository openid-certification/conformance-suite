package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ValidateClientAssertionClaims extends AbstractCondition {

	// TODO: make this configurable
	private int timeSkewMillis = 5 * 60 * 1000; // 5 minute allowable skew for testing
	private long oneDayMillis = 60 * 60 * 24 * 1000L; // Duration for one day

	@Override
	@PreEnvironment(required = { "server", "client" })
	public Environment evaluate(Environment env) {


		Instant now = Instant.now(); // to check timestamps

		String clientId = env.getString("client", "client_id"); // to check the client
		String issuer = env.getString("server", "issuer"); // to validate the issuer

		// check all our testable values
		if (Strings.isNullOrEmpty(clientId)
			|| Strings.isNullOrEmpty(issuer)) {
			throw error("Couldn't find issuer or client or token endpoint values in the test configuration to test the assertion");
		}

		JsonElement iss = env.getElementFromObject("client_assertion", "claims.iss");
		if (iss == null) {
			throw error("Missing iss");
		}
		if (!clientId.equals(env.getString("client_assertion", "claims.iss"))) {
			throw error("Issuer mismatch", args("expected", clientId, "actual", env.getString("client_assertion", "claims.iss")));
		}

		validateAud(env);

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

	protected void validateAud(Environment env) {
		String issuer = env.getString("server", "issuer");

		String tokenEndpoint = env.getString("server", "token_endpoint");
		if (Strings.isNullOrEmpty(tokenEndpoint)) {
			throw error("Couldn't find issuer or client or token endpoint values in the test configuration to test the assertion");
		}

		String mtlsTokenEndpoint = env.getString("server", "mtls_endpoint_aliases.token_endpoint");
		JsonElement aud = env.getElementFromObject("client_assertion", "claims.aud");
		if (aud == null) {
			throw error("Missing aud");
		}

		List<String> tokenEndpoints = new ArrayList<>(List.of(tokenEndpoint));
		if (mtlsTokenEndpoint != null) {
			tokenEndpoints.add(mtlsTokenEndpoint);
		}

		if (aud.isJsonArray()) {
			if (!aud.getAsJsonArray().contains(new JsonPrimitive(issuer)) &&
				!aud.getAsJsonArray().contains(new JsonPrimitive(tokenEndpoint)) &&
				!aud.getAsJsonArray().contains(new JsonPrimitive(mtlsTokenEndpoint))) {
				throw error("aud not found", args("expected", tokenEndpoints, "actual", aud));
			}
		} else {
			String audStr = OIDFJSON.getString(aud);
			if (!audStr.equals(issuer) &&
				!audStr.equals(tokenEndpoint) &&
				!audStr.equals(mtlsTokenEndpoint)) {
				throw error("aud mismatch", args("expected", tokenEndpoints, "actual", aud));
			}
		}
	}
}
