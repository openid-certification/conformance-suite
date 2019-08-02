package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.JWSVerifierFactory;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.proc.SimpleSecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.testmodule.OIDFJSON;

import java.security.Key;
import java.text.ParseException;
import java.util.List;
import java.util.regex.Pattern;

public abstract class AbstractValidateJWKs extends AbstractCondition {

	protected void checkJWKs(JsonElement jwks) {

		checkValidStructureInJwks(jwks);

		JsonArray jwksKeyArray = jwks.getAsJsonObject().getAsJsonArray("keys");
		jwksKeyArray.forEach(keyJsonElement -> {
			JsonObject keyObject = keyJsonElement.getAsJsonObject();

			checkMissingKey(keyObject, "kty");
			String kty = OIDFJSON.getString(keyObject.getAsJsonPrimitive("kty"));

			if ("RSA".equals(kty)) {

				validate(jwks, keyObject, "e", "n");

			} else if ("EC".equals(kty)) {

				validate(jwks, keyObject, "x", "y");

			}
		});
	}

	private void validate(JsonElement jwks, JsonObject keyObject, String... keys) {
		checkMissingKey(keyObject, keys);

		verifyKeysIsBase64UrlEncoded(keyObject, keys);

		// In case JWK is private key, then check valid d key. And verify private exponent matches public exponent.
		if (keyObject.has("d")) {
			verifyKeysIsBase64UrlEncoded(keyObject, "d");

			checkValidKey(jwks);
		}
	}

	private void checkValidKey(JsonElement jwks) {
		JsonObject claimObject = new JsonObject();
		claimObject.addProperty("test", "does private/public exponent match?");

		try {
			JWTClaimsSet claimSet = JWTClaimsSet.parse(claimObject.toString());
			JWKSet jwkSet = JWKSet.parse(jwks.toString());
			// sign jwt using private key
			SignedJWT jwt = signJWT(jwkSet, claimSet);

			// Verify JWT after signed to check valid JWKs
			verifyJWTAfterSigned(jwkSet, jwt);
		} catch (JOSEException | ParseException e) {
			throw error("Error validating JWKs", e);
		}
	}

	private SignedJWT signJWT(JWKSet jwkSet, JWTClaimsSet claimSet) throws JOSEException {
		JWK jwk = jwkSet.getKeys().iterator().next();

		JWSSigner signer = null;
		if (jwk.getKeyType().equals(KeyType.RSA)) {
			signer = new RSASSASigner((RSAKey) jwk);
		} else if (jwk.getKeyType().equals(KeyType.EC)) {
			signer = new ECDSASigner((ECKey) jwk);
		}

		if (signer == null) {
			throw error("Couldn't create signer from key", args("jwk", jwk.toJSONString()));
		}

		Algorithm alg = jwk.getAlgorithm();
		if (alg == null) {
			throw error("key should contain an 'alg' entry", args("jwk", jwk.toJSONString()));
		}

		JWSHeader header = new JWSHeader(JWSAlgorithm.parse(alg.getName()), JOSEObjectType.JWT, null, null, null, null, null, null, null, null, jwk.getKeyID(), null, null);

		SignedJWT jwt = new SignedJWT(header, claimSet);

		jwt.sign(signer);
		return jwt;
	}

	private void verifyJWTAfterSigned(JWKSet jwkSet, SignedJWT jwt) throws JOSEException {
		SecurityContext context = new SimpleSecurityContext();

		JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);

		JWSKeySelector<SecurityContext> selector = new JWSVerificationKeySelector<>(jwt.getHeader().getAlgorithm(), jwkSource);

		List<? extends Key> keys = selector.selectJWSKeys(jwt.getHeader(), context);
		for (Key key : keys) {
			JWSVerifierFactory factory = new DefaultJWSVerifierFactory();
			JWSVerifier verifier = factory.createJWSVerifier(jwt.getHeader(), key);

			if (!jwt.verify(verifier)) {
				throw error("Invalid JWKs. Private and public exponent don't match");
			}
		}
	}

	private void checkValidStructureInJwks(JsonElement jwks) {
		if (jwks == null) {
			throw error("Couldn't find JWKs in client configuration");
		} else if (!(jwks instanceof JsonObject)) {
			throw error("Invalid JWKs in client configuration - JSON decode failed");
		}

		if (!jwks.getAsJsonObject().has("keys") || !jwks.getAsJsonObject().get("keys").isJsonArray()) {
			throw error("Keys array not found in JWKs");
		}
	}

	private void verifyKeysIsBase64UrlEncoded(JsonObject keyObject, String... keys) {
		for (String key : keys) {
			String value = keyObject.get(key).getAsString();
			String regex = "[a-zA-Z0-9_-]";
			for (char character : value.toCharArray()) {
				if (!Pattern.matches(regex, String.valueOf(character))) {
					throw error(String.format("Value of key %s is invalid because it contains the character %s that is not permitted in unpadded base64url", key, character));
				}
			}
		}
	}

	private void checkMissingKey(JsonObject jsonObject, String... keys) {
		for (String key : keys) {
			if (!jsonObject.has(key)) {
				throw error("Key missing required field", args("missing key", key));
			}
		}
	}
}
