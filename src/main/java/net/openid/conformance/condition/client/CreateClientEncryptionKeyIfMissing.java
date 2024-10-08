package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

public class CreateClientEncryptionKeyIfMissing extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client")
	public Environment evaluate(Environment env) {
		JsonObject jwks = (JsonObject) env.getElementFromObject("client", "jwks");
		if (jwks == null) {
			jwks = new JsonObject();
			jwks.add("keys", new JsonArray());
			env.putObject("client", "jwks", jwks);
		}
		JsonArray keys = jwks.getAsJsonArray("keys");

		for (JsonElement jwkEl: keys) {
			JsonObject jwk = jwkEl.getAsJsonObject();
			if (!jwk.has("use")) {
				continue;
			}
			String use = OIDFJSON.getString(jwk.get("use"));
			if (use.equals("enc")) {
				log("Client JWKS already has a key with 'use': 'enc'", args("jwks", jwks));
				return env;
			}
		}

		String alg = env.getString("client", "authorization_encrypted_response_alg");
		if (alg==null) {
			// fairly arbitrary choice that happens to match ISO 18013-7
			alg = "ECDH-ES";
		}
		JWK jwk;

		try {
			jwk = createJwkForAlg(alg);
		} catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
			throw error("Failed to create key", e);
		}

		keys.add(JsonParser.parseString(jwk.toJSONString()));

		logSuccess("No client encryption key provided in client jwks in configuration so created an encryption key",
			args("jwk", jwk));
		return env;
	}

	private JWK createJwkForAlg(String alg) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
		JWK jwk;
		JWEAlgorithm jweAlgorithm = JWEAlgorithm.parse(alg);

		if (JWEAlgorithm.Family.RSA.contains(jweAlgorithm)) {
			KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
			gen.initialize(2048);
			KeyPair keyPair = gen.generateKeyPair();

			jwk = new RSAKey.Builder((RSAPublicKey)keyPair.getPublic())
				.privateKey((RSAPrivateKey)keyPair.getPrivate())
				.keyUse(KeyUse.ENCRYPTION)
				.algorithm(jweAlgorithm)
				.build();
		} else if (JWEAlgorithm.Family.ECDH_ES.contains(jweAlgorithm)) {
			KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
			gen.initialize(Curve.P_256.toECParameterSpec());
			KeyPair keyPair = gen.generateKeyPair();

			 jwk = new ECKey.Builder(Curve.P_256, (ECPublicKey) keyPair.getPublic())
				.privateKey((ECPrivateKey) keyPair.getPrivate())
				 .keyUse(KeyUse.ENCRYPTION)
				 .algorithm(jweAlgorithm)
				.build();

		} else {
			throw error("Not sure what kind of key to create for '%s' - please provide an encryption key in the test configuration.".formatted(alg));
		}
		return jwk;
	}

}
