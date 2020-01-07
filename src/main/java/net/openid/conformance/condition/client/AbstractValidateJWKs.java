package net.openid.conformance.condition.client;

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
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.OIDFJSON;

import java.security.Key;
import java.text.ParseException;
import java.util.List;
import java.util.regex.Pattern;

public abstract class AbstractValidateJWKs extends AbstractCondition {

	protected void checkJWKs(JsonElement jwks, boolean checkPrivatePart) {

		checkValidStructureInJwks(jwks);

		JsonArray jwksKeyArray = jwks.getAsJsonObject().getAsJsonArray("keys");
		jwksKeyArray.forEach(keyJsonElement -> {
			JsonObject keyObject = keyJsonElement.getAsJsonObject();

			checkMissingKey(keyObject, "kty");
			String kty = OIDFJSON.getString(keyObject.getAsJsonPrimitive("kty"));

			if ("RSA".equals(kty)) {

				checkMissingKey(keyObject, "e", "n");

				verifyKeysIsBase64UrlEncoded(keyObject, "e", "n");

				if (checkPrivatePart) verifyPrivatePart(jwks, keyObject);
			} else if ("EC".equals(kty)) {

				checkMissingKey(keyObject, "x", "y");

				verifyKeysIsBase64UrlEncoded(keyObject, "x", "y");

				if (checkPrivatePart) verifyPrivatePart(jwks, keyObject);
			}

		});
	}

	private void verifyPrivatePart(JsonElement jwks, JsonObject keyObject) {
		if (!keyObject.has("d")) {
			throw error("The JWK supplied in the configuration seems to be a public key (the 'd' key is missing). You must supply a private key in the test configuration.", args("jwk", keyObject));
		}

		verifyKeysIsBase64UrlEncoded(keyObject, "d");

		checkValidKey(jwks);
	}

	private void checkValidKey(JsonElement jwks) {
		JsonObject claimObject = new JsonObject();
		claimObject.addProperty("test", "does private/public exponent match?");

		try {
			JWTClaimsSet claimSet = JWTClaimsSet.parse(claimObject.toString());
			JWKSet jwkSet = JWKSet.parse(jwks.toString());
			if (jwkSet.getKeys().size() == 1) {
				// sign jwt using private key
				SignedJWT jwt = signJWT(jwkSet, claimSet);

				// Verify JWT after signed to check valid JWKs
				verifyJWTAfterSigned(jwkSet, jwt);
			} else {
				throw error("Expected only one JWK in the set", args("found", jwkSet.getKeys().size()));
			}
		} catch (JOSEException | ParseException e) {
			throw error("Error validating JWKS", ex(e, args("jwks", jwks)));
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
			throw error("keys supplied in the test configuration must contain an 'alg' entry that indicates which algorithm will be used for the test (e.g. 'PS256')", args("jwk", jwk.toJSONString()));
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
				throw error("Invalid JWKs supplied in configuration. Private and public exponent don't match (test JWS could not be verified)", args("jws", jwt.toString(), "jwks", jwkSet.toString()));
			}
		}
	}

	private void checkValidStructureInJwks(JsonElement jwks) {
		if (jwks == null) {
			throw error("Couldn't find JWKs in configuration");
		} else if (!(jwks instanceof JsonObject)) {
			throw error("Invalid JWKs in configuration - not a JSON object");
		}

		if (!jwks.getAsJsonObject().has("keys") || !jwks.getAsJsonObject().get("keys").isJsonArray()) {
			throw error("Keys array not found in JWKs", args("jwks", jwks));
		}
	}

	private void verifyKeysIsBase64UrlEncoded(JsonObject keyObject, String... keys) {
		for (String key : keys) {
			String value = OIDFJSON.getString(keyObject.get(key));
			String regex = "[a-zA-Z0-9_-]";
			for (char character : value.toCharArray()) {
				if (!Pattern.matches(regex, String.valueOf(character))) {
					throw error(String.format("Value of key %s is invalid because it contains the character %s that is not permitted in unpadded base64url", key, character), args("jwk", keyObject));
				}
			}
		}
	}

	private void checkMissingKey(JsonObject jsonObject, String... keys) {
		for (String key : keys) {
			if (!jsonObject.has(key)) {
				throw error("Key missing required field", args("jwk", jsonObject, "missing", key));
			}
		}
	}
}
