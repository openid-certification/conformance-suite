package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.JWKGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;

public class GenerateDpopKey extends AbstractGenerateClientJWKs {

	@Override
	@PreEnvironment(required = {"client"})
	public Environment evaluate(Environment env) {
		// We should allow some configuration of the algorithm used, or at least respect the server's metadata
		// (dpop_signing_alg_values_supported) on what it supports.
		JWKGenerator<RSAKey> generator = new RSAKeyGenerator(DEFAULT_KEY_SIZE)
				.algorithm(JWSAlgorithm.PS256);

		JWK key;
		try {
			key = ((JWKGenerator<? extends JWK>) generator).keyUse(KeyUse.SIGNATURE).generate();
		} catch (JOSEException e) {
			throw error("Failed to generate RSA key", e);
		}

		JsonObject keyJson = JsonParser.parseString(key.toJSONString()).getAsJsonObject();
		env.putObject("client", "dpop_private_jwk", keyJson);

		logSuccess("Generated dPOP JWKs", args("private_jwk", keyJson));

		return env;
	}

}
