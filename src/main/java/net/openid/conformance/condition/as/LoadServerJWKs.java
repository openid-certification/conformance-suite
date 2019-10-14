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

public class LoadServerJWKs extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = { "server_public_jwks", "server_jwks" })
	public Environment evaluate(Environment env) {

		JsonElement configured = env.getElementFromObject("config", "server.jwks");

		if (configured == null) {
			throw error("Couldn't find a JWK set in configuration");
		}

		// parse the JWKS to make sure it's valid
		try {
			JWKSet jwks = JWKSet.parse(configured.toString());

			JsonObject publicJwks = new JsonParser().parse(jwks.toJSONObject(true).toJSONString()).getAsJsonObject();
			JsonObject privateJwks = new JsonParser().parse(jwks.toJSONObject(false).toJSONString()).getAsJsonObject();

			env.putObject("server_public_jwks", publicJwks);
			env.putObject("server_jwks", privateJwks);

			logSuccess("Parsed public and private JWK sets", args("server_public_jwks", publicJwks, "server_jwks", jwks));

			return env;

		} catch (ParseException e) {
			throw error("Failure parsing JWK Set", e, args("jwk_string", configured));
		}

	}

}
