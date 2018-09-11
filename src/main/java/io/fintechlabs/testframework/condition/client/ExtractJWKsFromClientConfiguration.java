package io.fintechlabs.testframework.condition.client;

import java.text.ParseException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.JWKSet;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class ExtractJWKsFromClientConfiguration extends AbstractCondition {

	public ExtractJWKsFromClientConfiguration(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "client")
	@PostEnvironment(required = "client_jwks")
	public Environment evaluate(Environment env) {

		if (!env.containsObject("client")) {
			throw error("Couldn't find client configuration");
		}

		// bump the client's internal JWK up to the root
		JsonElement jwks = env.getElementFromObject("client", "jwks");

		if (jwks == null) {
			throw error("Couldn't find JWKs in client configuration");
		} else if (!(jwks instanceof JsonObject)) {
			throw error("Invalid JWKs in client configuration - JSON decode failed");
		}

		JWKSet parsed;

		try {
			parsed = JWKSet.parse(jwks.toString());
		} catch (ParseException e) {
			throw error("Invalid JWKs in client configuration (private key is required), JWKSet.parse failed",
				e, args("client_jwks", jwks));
		}

		JWKSet pub = parsed.toPublicJWKSet();

		JsonObject pubObj = (new JsonParser().parse(pub.toString())).getAsJsonObject();

		logSuccess("Extracted client JWK", args("client_jwks", jwks, "public_client_jwks", pubObj));

		env.putObject("client_jwks", jwks.getAsJsonObject());
		env.putObject("client_public_jwks", pubObj.getAsJsonObject());

		return env;
	}

}
