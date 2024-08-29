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

		JsonObject pubObj = JsonParser.parseString(pub.toString()).getAsJsonObject();

		logSuccess("Extracted client JWK", args("client_jwks", jwks, "public_client_jwks", pubObj));

		env.putObject("client_jwks", jwks.getAsJsonObject());
		env.putObject("client_public_jwks", pubObj.getAsJsonObject());
	}
}
