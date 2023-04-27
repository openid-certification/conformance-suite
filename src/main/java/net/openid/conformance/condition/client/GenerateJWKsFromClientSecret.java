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
		byte[] clientSecretBytes = clientSecret.getBytes();

		//address issue #1196
		// client secret might be shorter than the required size to be used to sign
		int minSize;
		switch(alg.toUpperCase()) {
			case "HS256":
				minSize = 32;
				break;
			case "HS384":
				minSize = 48;
				break;
			default:
				minSize = 64;
				break;
		}
		if(clientSecretBytes.length < minSize){
			throw error("The client secret configured in the test plan is too short to sign a JWT with. The " +
					alg.toUpperCase() + " requires a secret with at least " + minSize +
					" bytes and the provided secret is " + clientSecretBytes.length + " bytes.");
		}

		JWK jwk = new OctetSequenceKey.Builder(clientSecretBytes)
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
