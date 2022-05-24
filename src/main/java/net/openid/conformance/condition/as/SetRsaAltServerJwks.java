package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.JWKGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Checks the config for the server jwk.
 * If a signing RSA key exists, it's also used as the alt RSA signing key
 * If the key is not a RSA key, a new RSA key is generated and set as the alt signing JWK.
 * The new public key is also added to the server's list of public keys in the jwks_uri
 */
public class SetRsaAltServerJwks extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	//may also add server_encryption_keys if encryption keys are found, optional
	@PostEnvironment(required = { "server_public_jwks", "server_jwks", "server_alt_jwks" })
	public Environment evaluate(Environment env) {

		JsonElement configured = env.getElementFromObject("config", "server.jwks");

		if (configured == null) {
			throw error("Couldn't find a JWK set in configuration");
		}

		// parse the JWKS to make sure it's valid
		try {
			JWKSet jwks = JWKUtil.parseJWKSet(configured.toString());

			JsonObject publicJwks = JWKUtil.getPublicJwksAsJsonObject(jwks);
			JsonObject privateJwks = JWKUtil.getPrivateJwksAsJsonObject(jwks);
			JWK altPrivateKey = null;
			boolean foundSigningRsaKey = false;

			for(JWK jwkKey : jwks.getKeys()) {
				boolean isSigningKey = false;
				if(null == jwkKey.getKeyUse()) { // not set,so can be use for sig or enc
					isSigningKey = true;
				} else {
					if(KeyUse.SIGNATURE.equals(jwkKey.getKeyUse())) {
						isSigningKey = true;
					}
				}
				if(KeyType.RSA.equals(jwkKey.getKeyType()) && isSigningKey) {
					foundSigningRsaKey = true;
					altPrivateKey = jwkKey; // use existing key
				}
			}

			if(!foundSigningRsaKey) {  // generated new RSA key and add public keys
				JWKGenerator<? extends JWK> jwkGenerator = new RSAKeyGenerator(RSAKeyGenerator.MIN_KEY_SIZE_BITS);
				jwkGenerator.keyID(UUID.randomUUID().toString())
				.keyUse(KeyUse.SIGNATURE)
				.algorithm(JWSAlgorithm.RS256);
				altPrivateKey = jwkGenerator.generate();

				List<JWK> newJwksList = new ArrayList<>(jwks.getKeys());
				newJwksList.add(altPrivateKey);
				JWKSet newJwkSet = new JWKSet(newJwksList);
				publicJwks = JWKUtil.getPublicJwksAsJsonObject(newJwkSet);

				env.putObject("server_public_jwks", publicJwks);
				log("generated new alt RSA key configuration");
			}
			setAltPrivateKeyJwk(env, altPrivateKey);

			logSuccess("Set alt server key",
				args("server_public_jwks", publicJwks, "server_jwks", privateJwks, "server_alt_jwks", JWKUtil.getPrivateJwksAsJsonObject(new JWKSet(altPrivateKey))));

			return env;

		} catch (ParseException e) {
			throw error("Failure parsing JWK Set", e, args("jwk_string", configured));
		} catch(JOSEException e) {
			throw error("Failed to generate new key", e);
		}

	}

	private void setAltPrivateKeyJwk(Environment env, JWK jwk) {
		JWKSet keySet =  new JWKSet(jwk);
		JsonObject privateJwks = JWKUtil.getPrivateJwksAsJsonObject(keySet);
		env.putObject("server_alt_jwks", privateJwks);
	}

}
