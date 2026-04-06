package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.JWKSet;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

public abstract class AbstractExtractJWKsFromClientConfiguration extends AbstractCondition {
	protected void extractJwks(Environment env, JsonElement jwks) {
		extractJwks(env, jwks, "client_jwks", "client_public_jwks");
	}

	protected void extractJwks(Environment env, JsonElement jwks, String jwksKey, String publicJwksKey) {

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
				e, args(jwksKey, jwks));
		}

		JWKSet pub = parsed.toPublicJWKSet();

		JsonObject pubObj = JsonParser.parseString(pub.toString()).getAsJsonObject();

		logSuccess("Extracted client JWK", args(jwksKey, jwks, publicJwksKey, pubObj));

		env.putObject(jwksKey, jwks.getAsJsonObject());
		env.putObject(publicJwksKey, pubObj.getAsJsonObject());
	}
}
