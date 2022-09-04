package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.nimbusds.jose.jwk.JWKSet;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

public class CheckServerKeysIsValid extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server_jwks")
	public Environment evaluate(Environment env) {
		JsonObject serverJWKs = env.getObject("server_jwks");

		if (serverJWKs == null) {
			throw error("Couldn't find server JWKs");
		}

		try {
			// parse the key to make sure it's really a JWK
			JWKSet.parse(serverJWKs.toString());
		} catch (ParseException e) {
			throw error("Unable to parse JWK set", e);
		}

		logSuccess("Server JWKs is valid", args("server_jwks", serverJWKs));
		return env;
	}
}
