package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.time.Instant;
import java.util.Date;

public class ValidateRequestObjectClaims extends AbstractCondition {

	protected int timeSkewMillis = 5 * 60 * 1000; // 5 minute allowable skew for testing

	@Override
	@PreEnvironment(required = {"authorization_request_object", "client"})
	public Environment evaluate(Environment env) {

		String clientId = env.getString("client", "client_id"); // to check the issuer
		String issuer = env.getString("server", "issuer"); // to validate the audience
		Instant now = Instant.now(); // to check timestamps

		// check all our testable values
		if (Strings.isNullOrEmpty(clientId)
			|| Strings.isNullOrEmpty(issuer)) {
			throw error("Couldn't find values to test request object against");
		}

		String iss = env.getString("authorization_request_object", "claims.iss");
		if (iss == null) {
			throw error("Missing issuer");
		}

		if (!clientId.equals(iss)) {
			throw error("Issuer mismatch", args("expected", clientId, "actual", iss));
		}

		validateAud(env);

		Long exp = env.getLong("authorization_request_object", "claims.exp");
		if (exp == null) {
			log(args("msg", "Missing expiration", "result", ConditionResult.INFO));
		} else {
			if (now.minusMillis(timeSkewMillis).isAfter(Instant.ofEpochSecond(exp))) {
				throw error("Token expired", args("expiration", new Date(exp * 1000L), "now", now));
			}
		}

		validateIat(env, now);

		Long nbf = env.getLong("authorization_request_object", "claims.nbf");
		if (nbf != null) {
			if (now.plusMillis(timeSkewMillis).isBefore(Instant.ofEpochSecond(nbf))) {
				// this is just something to log, it doesn't make the token invalid
				log("Token has future not-before", args("not-before", new Date(nbf * 1000L), "now", now));
			}
		}
		//Also see CreateEffectiveAuthorizationRequestParameters for max_age processing
		JsonElement maxAgeElement  = env.getElementFromObject("authorization_request_object", "claims.max_age");
		if (maxAgeElement == null) {
			log("Request object does not contain a max_age claim");
		} else if(maxAgeElement.isJsonNull()) {
			//EnsureNumericRequestObjectClaimsAreNotNull handles the JsonNull case
			//Additionally, CreateEffectiveAuthorizationRequestParameters completely ignores max_age when it is json null
		} else {
			try {
				Number maxAge = OIDFJSON.getNumber(maxAgeElement);
				log("max_age is correctly encoded as a number", args("max_age", maxAge));
			} catch (OIDFJSON.UnexpectedJsonTypeException ex) {
				throw error("max_age is not encoded as a number", args("max_age", maxAgeElement));
			}
		}

		validateJti(env);

		logSuccess("Request object claims passed all validation checks");
		return env;
	}

	protected void validateAud(Environment env) {
		JsonElement aud = env.getElementFromObject("authorization_request_object", "claims.aud");
		String issuer = env.getString("server", "issuer");

		if (aud == null) {
			throw error("Missing audience");
		}

		if (aud.isJsonArray()) {
			if (!aud.getAsJsonArray().contains(new JsonPrimitive(issuer))) {
				throw error("Audience not found", args("expected", issuer, "actual", aud));
			}
		} else {
			if (!issuer.equals(OIDFJSON.getString(aud))) {
				throw error("Audience mismatch", args("expected", issuer, "actual", aud));
			}
		}
	}

	protected void validateIat(Environment env, Instant now) {
		Long iat = env.getLong("authorization_request_object", "claims.iat");
		if (iat == null) {
			log(args("msg", "Missing issuance time", "result", ConditionResult.INFO));
		} else {

			if (now.plusMillis(timeSkewMillis).isBefore(Instant.ofEpochSecond(iat))) {
				throw error("Token issued in the future", args("issued-at", new Date(iat * 1000L), "now", now));
			}
		}
	}

	protected void validateJti(Environment env) {
		JsonElement jti = env.getElementFromObject("authorization_request_object", "claims.jti");
		boolean isPresent = jti != null && !jti.isJsonNull();
		if (isPresent) {
			boolean isString = jti.isJsonPrimitive() && jti.getAsJsonPrimitive().isString();
			if (!isString) {
				throw error("jti must be a string when present", args("jti", jti));
			}
		}
	}
}
