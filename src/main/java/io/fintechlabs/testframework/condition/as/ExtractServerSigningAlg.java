package io.fintechlabs.testframework.condition.as;

import com.google.gson.JsonObject;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

import java.text.ParseException;

public class ExtractServerSigningAlg extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server_jwks")
	@PostEnvironment(strings = "signing_algorithm")
	public Environment evaluate(Environment env) {

		JsonObject jwks = env.getObject("server_jwks");

		try {
			JWKSet jwkSet = JWKSet.parse(jwks.toString());

			if (jwkSet.getKeys().size() == 1) {
				JWK jwk = jwkSet.getKeys().iterator().next();
				Algorithm alg = jwk.getAlgorithm();
				if (alg == null) {
					throw error("No algorithm specified for key", args("jwk", jwk.toJSONString()));
				} else {
					env.putString("signing_algorithm", alg.toString());

					logSuccess("Successfully extracted algorithm", args("signing_algorithm", alg.toString()));

					return env;
				}

			} else {
				throw error("Expected only one JWK in the set", args("found", jwkSet.getKeys().size()));
			}

		} catch (ParseException e) {
			throw error(e);

		}

	}

}
