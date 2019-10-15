package net.openid.conformance.condition.client;

import com.google.common.base.Strings;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateJwksUri extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "base_url")
	@PostEnvironment(strings = "jwks_uri")
	public Environment evaluate(Environment env) {
		String baseUrl = env.getString("base_url");

		if (Strings.isNullOrEmpty(baseUrl)) {
			throw error("Base URL was null or empty");
		}

		// calculate the redirect URI based on our given base URL
		String jwksUri = baseUrl + "/jwks";
		env.putString("jwks_uri", jwksUri);

		logSuccess("Created JWKs URI",
			args("jwks_uri", jwksUri));

		return env;
	}

}
