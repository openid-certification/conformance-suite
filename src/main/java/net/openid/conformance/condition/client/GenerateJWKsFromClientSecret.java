package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetSequenceKey;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;

public class GenerateJWKsFromClientSecret extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client")
	@PostEnvironment(required = "client_jwks")
	public Environment evaluate(Environment env) {
		String clientSecret = env.getString("client", "client_secret");

		if (Strings.isNullOrEmpty(clientSecret)) {
			throw error("Couldn't find client secret");
		}

		String alg = env.getString("client", "client_secret_jwt_alg");
		if (Strings.isNullOrEmpty(alg)) {
			alg = JWSAlgorithm.HS256.getName();
		}

		// generate a JWK Set for the client's secret
		JWK jwk = new OctetSequenceKey.Builder(clientSecret.getBytes())
			.algorithm(JWSAlgorithm.parse(alg))
			.keyUse(KeyUse.SIGNATURE)
			// no key ID
			.build();

		JWKSet jwks = new JWKSet(jwk);

		JsonObject reparsed = JWKUtil.getPrivateJwksAsJsonObject(jwks);

		env.putObject("client_jwks", reparsed);

		logSuccess("Generated JWK Set from symmetric key", args("client_jwks", reparsed));

		return env;

	}

}
