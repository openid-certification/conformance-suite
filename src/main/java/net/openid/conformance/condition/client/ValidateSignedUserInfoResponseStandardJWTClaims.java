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

public class ValidateSignedUserInfoResponseStandardJWTClaims extends AbstractCondition {

	public static final String USERINFO_OBJECT = "userinfo_object";
	// TODO: make this configurable
	private int timeSkewMillis = 5 * 60 * 1000; // 5 minute allowable skew for testing

	@Override
	@PreEnvironment(required = { USERINFO_OBJECT, "server", "client" } )
	public Environment evaluate(Environment env) {

		String clientId = env.getString("client", "client_id"); // to check the audience
		String issuer = env.getString("server", "issuer"); // to validate the issuer
		Instant now = Instant.now(); // to check timestamps

		// check all our testable values
		if (Strings.isNullOrEmpty(clientId)
			|| Strings.isNullOrEmpty(issuer)) {
			throw error("Couldn't find values to test ID token against");
		}

		JsonElement iss = env.getElementFromObject(USERINFO_OBJECT, "claims.iss");
		if (iss == null) {
			throw error("Missing issuer");
		}

		if (!issuer.equals(env.getString(USERINFO_OBJECT, "claims.iss"))) {
			throw error("Issuer mismatch", args("expected", issuer, "actual", env.getString(USERINFO_OBJECT, "claims.iss")));
		}

		// sub is checked in AbstractVerifyUserInfoAndIdTokenSameSub

		JsonElement aud = env.getElementFromObject(USERINFO_OBJECT, "claims.aud");
		if (aud == null) {
			throw error("Missing audience");
		}

		if (aud.isJsonArray()) {
			if (!aud.getAsJsonArray().contains(new JsonPrimitive(clientId))) {
				throw error("Audience not found", args("expected", clientId, "actual", aud));
			}
		} else if (aud.isJsonPrimitive() && aud.getAsJsonPrimitive().isString()) {
			if (!clientId.equals(OIDFJSON.getString(aud))) {
				throw error("Audience mismatch", args("expected", clientId, "actual", aud));
			}
		} else {
			throw error("'aud' is neither a string nor an array");
		}

		Long exp = env.getLong(USERINFO_OBJECT, "claims.exp");
		if (exp != null) {
			if (now.minusMillis(timeSkewMillis).isAfter(Instant.ofEpochSecond(exp))) {
				throw error("response expired", args("expiration", new Date(exp * 1000L), "now", now));
			}
		}

		Long iat = env.getLong(USERINFO_OBJECT, "claims.iat");
		if (iat != null) {
			if (now.plusMillis(timeSkewMillis).isBefore(Instant.ofEpochSecond(iat))) {
				throw error("response issued in the future", args("issued-at", new Date(iat * 1000L), "now", now));
			}
		}

		Long nbf = env.getLong(USERINFO_OBJECT, "claims.nbf");
		if (nbf != null) {
			if (now.plusMillis(timeSkewMillis).isBefore(Instant.ofEpochSecond(nbf))) {
				throw error("response has future not-before", args("not-before", new Date(nbf * 1000L), "now", now));
			}
		}

		// jti - also not mentioned in spec (but defined in JWT); not currently checked

		logSuccess("Signed userinfo response iss and aud claims passed validation checks. If present, exp, iat and nbf are also valid.");
		return env;

	}

}
