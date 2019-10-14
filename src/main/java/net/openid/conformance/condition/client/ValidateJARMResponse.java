package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.time.Instant;
import java.util.Date;

public class ValidateJARMResponse extends AbstractCondition {

	// TODO: make this configurable
	private int timeSkewMillis = 5 * 60 * 1000; // 5 minute allowable skew for testing

	@Override
	@PreEnvironment(required = { "jarm_response", "server", "client" } )
	public Environment evaluate(Environment env) {

		String clientId = env.getString("client", "client_id"); // to check the audience
		String issuer = env.getString("server", "issuer"); // to validate the issuer
		Instant now = Instant.now(); // to check timestamps

		// check all our testable values
		if (Strings.isNullOrEmpty(clientId)
			|| Strings.isNullOrEmpty(issuer)) {
			throw error("Couldn't find values to test response against");
		}

		JsonElement iss = env.getElementFromObject("jarm_response", "claims.iss");
		if (iss == null) {
			throw error("Missing issuer");
		}

		if (!issuer.equals(env.getString("jarm_response", "claims.iss"))) {
			throw error("Issuer mismatch", args("expected", issuer, "actual", env.getString("jarm_response", "claims.iss")));
		}

		JsonElement aud = env.getElementFromObject("jarm_response", "claims.aud");
		if (aud == null) {
			throw error("Missing audience");
		}

		if (aud.isJsonArray()) {
			if (!aud.getAsJsonArray().contains(new JsonPrimitive(clientId))) {
				throw error("Audience not found", args("expected", clientId, "actual", aud));
			}
		} else {
			if (!clientId.equals(OIDFJSON.getString(aud))) {
				throw error("Audience mismatch", args("expected", clientId, "actual", aud));
			}
		}

		Long exp = env.getLong("jarm_response", "claims.exp");
		if (exp == null) {
			throw error("Missing expiration");
		} else {
			if (now.minusMillis(timeSkewMillis).isAfter(Instant.ofEpochSecond(exp))) {
				throw error("Token expired", args("expiration", new Date(exp * 1000L), "now", now));
			}
		}

		// iat and nbf are not required to be present, but should be valid if they are
		// https://bitbucket.org/openid/fapi/issues/269/jarm-response-contents-clarifications
		Long iat = env.getLong("jarm_response", "claims.iat");
		if (iat != null) {
			if (now.plusMillis(timeSkewMillis).isBefore(Instant.ofEpochSecond(iat))) {
				throw error("Token issued in the future", args("issued-at", new Date(iat * 1000L), "now", now));
			}
		}

		Long nbf = env.getLong("jarm_response", "claims.nbf");
		if (nbf != null) {
			if (now.plusMillis(timeSkewMillis).isBefore(Instant.ofEpochSecond(nbf))) {
				// this is just something to log, it doesn't make the token invalid
				log("Token has future not-before", args("not-before", new Date(nbf * 1000L), "now", now));
			}
		}

		logSuccess("JARM response standard JWT claims are valid");
		return env;

	}

}
