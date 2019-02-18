package io.fintechlabs.testframework.condition.as;

import java.time.Instant;
import java.util.Date;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class ValidateRequestObjectClaims extends AbstractCondition {

	// TODO: make this configurable
	private int timeSkewMillis = 5 * 60 * 1000; // 5 minute allowable skew for testing

	/**
	 * @param testId
	 * @param log
	 * @param conditionResultOnFailure
	 * @param requirements
	 */
	public ValidateRequestObjectClaims(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
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

		JsonElement aud = env.getElementFromObject("authorization_request_object", "claims.aud");
		if (aud == null) {
			throw error("Missing audience");
		}

		if (aud.isJsonArray()) {
			if (!aud.getAsJsonArray().contains(new JsonPrimitive(issuer))) {
				throw error("Audience not found", args("expected", issuer, "actual", aud));
			}
		} else {
			if (!issuer.equals(aud.getAsString())) {
				throw error("Audience mismatch", args("expected", issuer, "actual", aud));
			}
		}

		Long exp = env.getLong("authorization_request_object", "claims.exp");
		if (exp == null) {
			log(args("msg", "Missing expiration", "result", ConditionResult.INFO));
		} else {
			if (now.minusMillis(timeSkewMillis).isAfter(Instant.ofEpochSecond(exp))) {
				throw error("Token expired", args("expiration", new Date(exp * 1000L), "now", now));
			}
		}

		Long iat = env.getLong("authorization_request_object", "claims.iat");
		if (iat == null) {
			log(args("msg", "Missing issuance time", "result", ConditionResult.INFO));
		} else {
			if (now.plusMillis(timeSkewMillis).isBefore(Instant.ofEpochSecond(iat))) {
				throw error("Token issued in the future", args("issued-at", new Date(iat * 1000L), "now", now));
			}
		}

		Long nbf = env.getLong("authorization_request_object", "claims.nbf");
		if (nbf != null) {
			if (now.plusMillis(timeSkewMillis).isBefore(Instant.ofEpochSecond(nbf))) {
				// this is just something to log, it doesn't make the token invalid
				log("Token has future not-before", args("not-before", new Date(nbf * 1000L), "now", now));
			}
		}

		logSuccess("Request object claims passed all validation checks");
		return env;


	}

}
