package net.openid.conformance.condition.as;

import java.text.ParseException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.JWKSet;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractJWKsFromResourceConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource")
	@PostEnvironment(required = "resource_jwks")
	public Environment evaluate(Environment env) {

		if (!env.containsObject("resource")) {
			throw error("Couldn't find resource configuration");
		}

		// bump the client's internal JWK up to the root
		JsonElement jwks = env.getElementFromObject("resource", "jwks");

		if (jwks == null) {
			throw error("Couldn't find JWKs in resource configuration");
		} else if (!(jwks instanceof JsonObject)) {
			throw error("Invalid JWKs in resource configuration - JSON decode failed");
		}

		try {
			JWKSet parsed = JWKSet.parse(jwks.toString());
			JWKSet pub = parsed.toPublicJWKSet();

			JsonObject pubObj = (new JsonParser().parse(pub.toString())).getAsJsonObject();

			logSuccess("Extracted resource JWK", args("resource_jwks", jwks, "public_resource_jwks", pubObj));

			env.putObject("resource_jwks", jwks.getAsJsonObject());
			env.putObject("resource_public_jwks", pubObj.getAsJsonObject());

			return env;


		} catch (ParseException e) {
			throw error("Invalid JWKs in resource configuration, JWKS parsing failed", e, args("resource_jwks", jwks));
		}
	}

}
