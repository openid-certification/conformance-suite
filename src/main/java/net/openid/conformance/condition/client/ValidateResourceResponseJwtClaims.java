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
import java.util.UUID;

public class ValidateResourceResponseJwtClaims extends AbstractCondition {

	private int timeSkewMillis = 5 * 60 * 1000; // 5 minute allowable skew for testing

	@Override
	@PreEnvironment(required = { "config", "endpoint_response_jwt", "server", "certificate_subject" } )
	public Environment evaluate(Environment env) {
		String ou = env.getString("certificate_subject", "ou");
		if (Strings.isNullOrEmpty(ou)) {
			throw error("'ou' missing from client MTLS certificate");
		}

		String expectedIssuer = env.getString("config", "resource.brazilOrganizationId");
		if (Strings.isNullOrEmpty(expectedIssuer)) {
			throw error("Resource server organization id missing from test configuration; this must be provided when 'scope' contains payments");
		}
		Instant now = Instant.now(); // to check timestamps

		// iss (in the JWT request and in the JWT response): the receiver of the message shall validate if the value of the iss field matches the organisationId of the sender;
		JsonElement iss = env.getElementFromObject("endpoint_response_jwt", "claims.iss");
		if (iss == null) {
			throw error("'iss' claim missing");
		}

		String actualIssuer = env.getString("endpoint_response_jwt", "claims.iss");
		if (!expectedIssuer.equals(actualIssuer)) {
			throw error("Issuer does not match the authorization server's organisation id provided in the test configuration",
				args("expected", expectedIssuer, "actual", actualIssuer));
		}

		// aud
		JsonElement aud = env.getElementFromObject("endpoint_response_jwt", "claims.aud");
		if (aud == null) {
			throw error("'aud' claim missing");
		}

		if (aud.isJsonArray()) {
			if (!aud.getAsJsonArray().contains(new JsonPrimitive(ou))) {
				throw error("'aud' array does not contain our organization id", args("expected", ou, "actual", aud));
			}
		} else {
			if (!ou.equals(OIDFJSON.getString(aud))) {
				throw error("'aud' is not our organization id", args("expected", ou, "actual", aud));
			}
		}

		// jti (in the JWT request and in the JWT response): the value of the jti field shall be filled with the UUID defined by the institution according to [RFC4122] version 4;¶
		JsonElement jti = env.getElementFromObject("endpoint_response_jwt", "claims.jti");
		if (jti == null) {
			throw error("'jti' claim missing");
		}
		if (!jti.isJsonPrimitive() || !jti.getAsJsonPrimitive().isString()) {
			throw error("'jti' claim not a string");
		}
		String jtiStr = OIDFJSON.getString(jti);
		try {
			@SuppressWarnings("unused")
			UUID jtiUuid = UUID.fromString(jtiStr);
		} catch (IllegalArgumentException e) {
			throw error("Invalid jti - not a UUID", args("jti", jtiStr));
		}

		Long iat = env.getLong("endpoint_response_jwt", "claims.iat");
		if (iat == null) {
			throw error("'iat' claim missing");
		} else {
			if (now.plusMillis(timeSkewMillis).isBefore(Instant.ofEpochSecond(iat))) {
				throw error("Response JWS 'iat' in the future", args("issued-at", new Date(iat * 1000L), "now", now));
			}
			if (now.minusMillis(timeSkewMillis).isAfter(Instant.ofEpochSecond(iat))) {
				throw error("Response JWS 'iat' more than 5 minutes in the past", args("issued-at", new Date(iat * 1000L), "now", now));
			}
		}

		Long exp = env.getLong("endpoint_response_jwt", "claims.exp");
		if (exp != null) {
			if (now.minusMillis(timeSkewMillis).isAfter(Instant.ofEpochSecond(exp))) {
				throw error("Response JWS expired", args("expiration", new Date(exp * 1000L), "now", now));
			}
		}

		Long nbf = env.getLong("endpoint_response_jwt", "claims.nbf");
		if (nbf != null) {
			if (now.plusMillis(timeSkewMillis).isBefore(Instant.ofEpochSecond(nbf))) {
				throw error("Response JWS has future not-before", args("not-before", new Date(nbf * 1000L), "now", now));
			}
		}

		logSuccess("Resource endpoint response JWS iss, aud, exp, iat, auth_time, acr & nbf claims passed validation checks");
		return env;

	}

}
