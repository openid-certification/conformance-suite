package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractLenientJwksCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

public class CheckServerKeysIsValid extends AbstractLenientJwksCondition {

	@Override
	@PreEnvironment(required = "server_jwks")
	public Environment evaluate(Environment env) {
		JsonObject serverJWKs = env.getObject("server_jwks");

		if (serverJWKs == null) {
			throw error("Couldn't find server JWKs");
		}

		try {
			// parse to make sure it's really a JWK set; individual keys the JOSE library cannot use
			// are logged and skipped, as a recipient ignores them (RFC 7517 section 5)
			parseJwksLenientlyLoggingSkips(serverJWKs.toString(), "server");
		} catch (ParseException e) {
			throw error("Unable to parse JWK set", e);
		}

		logSuccess("Server JWKs is valid", args("server_jwks", serverJWKs));
		return env;
	}
}
