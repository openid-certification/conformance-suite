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

import static java.time.temporal.ChronoUnit.DAYS;

public class ValidateIdToken extends AbstractCondition {

	// TODO: make this configurable
	protected int timeSkewMillis = 5 * 60 * 1000; // 5 minute allowable skew for testing

	protected void verifyIat(Instant now, Long iat) {
		if (now.minusMillis(timeSkewMillis).isAfter(Instant.ofEpochSecond(iat))) {
			// as per OIDCC, the client can reasonably assume servers send iat values that match the current time:
			// "The iat Claim can be used to reject tokens that were issued too far away from the current time, limiting
			// the amount of time that nonces need to be stored to prevent attacks. The acceptable range is Client specific."
			throw error("Token 'iat' more than 5 minutes in the past", args("issued-at", new Date(iat * 1000L), "now", now));
		}
	}

	@Override
	@PreEnvironment(required = { "id_token", "server", "client" } )
	public Environment evaluate(Environment env) {

		String clientId = env.getString("client", "client_id"); // to check the audience
		String issuer = env.getString("server", "issuer"); // to validate the issuer
		Instant now = Instant.now(); // to check timestamps

		// check all our testable values
		if (Strings.isNullOrEmpty(clientId)
			|| Strings.isNullOrEmpty(issuer)) {
			throw error("Couldn't find values to test ID token against");
		}

		// checks in the order the claims are listed in https://openid.net/specs/openid-connect-core-1_0.html#IDToken

		JsonElement iss = env.getElementFromObject("id_token", "claims.iss");
		if (iss == null) {
			throw error("'iss' claim missing");
		}

		if (!issuer.equals(env.getString("id_token", "claims.iss"))) {
			throw error("Issuer mismatch", args("expected", issuer, "actual", env.getString("id_token", "claims.iss")));
		}

		// sub is checked in CheckForSubjectInIdToken

		JsonElement aud = env.getElementFromObject("id_token", "claims.aud");
		if (aud == null) {
			throw error("'aud' claim missing");
		}

		if (aud.isJsonArray()) {
			if (!aud.getAsJsonArray().contains(new JsonPrimitive(clientId))) {
				throw error("'aud' array does not contain our client id", args("expected", clientId, "actual", aud));
			}
		} else {
			if (!clientId.equals(OIDFJSON.getString(aud))) {
				throw error("'aud' is not our client id", args("expected", clientId, "actual", aud));
			}
		}

		Long exp = env.getLong("id_token", "claims.exp");
		if (exp == null) {
			throw error("'exp' claim missing");
		} else {
			if (now.minusMillis(timeSkewMillis).isAfter(Instant.ofEpochSecond(exp))) {
				throw error("Token expired", args("expiration", new Date(exp * 1000L), "now", now));
			}
		}

		Long iat = env.getLong("id_token", "claims.iat");
		if (iat == null) {
			throw error("'iat' claim missing");
		}
		if (now.plusMillis(timeSkewMillis).isBefore(Instant.ofEpochSecond(iat))) {
			throw error("Token 'iat' in the future", args("issued-at", new Date(iat * 1000L), "now", now));
		}
		verifyIat(now, iat);

		// auth_time - optional number
		Long authTime = env.getLong("id_token", "claims.auth_time");
		if (authTime != null) {
			if (now.minus(365, DAYS).isAfter(Instant.ofEpochSecond(authTime))) {
				throw error("id_token auth_time is over a year in the past", args("auth_time", new Date(authTime * 1000L), "now", now));
			}
			if (now.plusMillis(timeSkewMillis).isBefore(Instant.ofEpochSecond(authTime))) {
				throw error("id_token auth_time is in the future", args("auth_time", new Date(authTime * 1000L), "now", now));
			}
		}

		// nonce checked in ValidateIdTokenNonce

		// acr - optional string
		var acr = env.getString("id_token", "claims.acr");
		if (acr != null && acr.equals("")) {
			throw error("id_token acr is an empty string");
		}

		// amr - not currently checked

		// azp - not currently checked

		// nbf - not actually part of spec; but JWT defines known behaviour that really should be followed
		Long nbf = env.getLong("id_token", "claims.nbf");
		if (nbf != null) {
			if (now.plusMillis(timeSkewMillis).isBefore(Instant.ofEpochSecond(nbf))) {
				// this is just something to log, it doesn't make the token invalid
				log("Token has future not-before", args("not-before", new Date(nbf * 1000L), "now", now));
			}
		}

		// jti - also not mentioned in spec (but defined in JWT); not currently checked

		logSuccess("ID token iss, aud, exp, iat, auth_time, acr & nbf claims passed validation checks");
		return env;

	}

}
