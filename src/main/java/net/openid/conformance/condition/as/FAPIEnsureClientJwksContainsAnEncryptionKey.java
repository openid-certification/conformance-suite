package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWEUtil;

import java.text.ParseException;

public class FAPIEnsureClientJwksContainsAnEncryptionKey extends AbstractCondition {

	private static final String JWKS_KEY = "client_jwks";

	@SuppressWarnings("deprecation")
	@Override
	@PreEnvironment(required = {JWKS_KEY, "client"})
	public Environment evaluate(Environment env) {
		JsonObject jwks = env.getObject(JWKS_KEY);
		if (jwks == null) {
			throw error("Couldn't find "+ JWKS_KEY +" in environment");
		}
		String alg = env.getString("client", "id_token_encrypted_response_alg");
		if(alg==null) {
			throw error("id_token_encrypted_response_alg is not set");
		}
		JWEAlgorithm jweAlgorithm = JWEAlgorithm.parse(alg);
		JWKSet jwkset;
		try {
			jwkset = JWKSet.parse(jwks.toString());
		} catch (ParseException e) {
			throw error("Failure parsing "+ JWKS_KEY, e);
		}
		JWK key = JWEUtil.selectAsymmetricKeyForEncryption(jwkset, jweAlgorithm);
		if (key==null) {
			throw error("Failed to find an encryption key in client jwks",
				args("id_token_encrypted_response_alg", alg, "client_jwks", jwks));
		}
		if (JWEAlgorithm.RSA1_5.equals(key.getAlgorithm())) {
			throw error("RSA1_5 algorithm is not allowed",
				args("kid", (key.getKeyID()!=null?key.getKeyID():"not set"),
					"algorithm", key.getAlgorithm().toString()));
		}

		logSuccess("Found an encryption key in client jwks",
			args("kid", (key.getKeyID()!=null?key.getKeyID():"not set"), "algorithm", (key.getAlgorithm()!=null?key.getAlgorithm().toString():"not set")));
		return env;
	}

}
