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
import com.nimbusds.jose.crypto.Ed25519Signer;
import com.nimbusds.jose.crypto.Ed25519Verifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.AsymmetricJWK;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.SecretJWK;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSVerifierFactory;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.proc.SimpleSecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.extensions.AlternateJWSVerificationKeySelector;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWKUtil;

import java.security.KeyPair;
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

				if (checkPrivatePart) {
					verifyPrivatePart(jwks, keyObject);
				}
			} else if ("EC".equals(kty)) {

				checkMissingKey(keyObject, "x", "y");

				verifyKeysIsBase64UrlEncoded(keyObject, "x", "y");

				if (checkPrivatePart) {
					verifyPrivatePart(jwks, keyObject);
				}
			} else if("OKP".equals(kty)) {
				checkMissingKey(keyObject, "x", "crv");
				String crv = OIDFJSON.getString(keyObject.getAsJsonPrimitive("crv"));
				if(!Curve.Ed25519.getName().equals(crv)) {
					log("Jwks contains an unsupported curve", args("jwks", keyJsonElement));
				}

				verifyKeysIsBase64UrlEncoded(keyObject, "x");

				if (checkPrivatePart) {
					verifyPrivatePart(jwks, keyObject);
				}

			}
			parseJWKWithNimbus(keyObject);
		});
	}

	/**
	 * Nimbusds performs various checks (including the ones manually implemented in this class)
	 * @param keyObject
	 */
	protected void parseJWKWithNimbus(JsonObject keyObject) {
		try {
			//https://openid.net/specs/openid-connect-registration-1_0.html#rfc.section.2
			//jwks
			//    The JWK x5c parameter MAY be used to provide X.509 representations of keys provided.
			//    When used, the bare key values MUST still be present and MUST match those in the certificate
			//Nimbusds performs this check besides other checks like missing properties etc while parsing
			@SuppressWarnings("unused")

			JWK jwk = JWK.parse(keyObject.toString());
		} catch (ParseException ex) {
			throw error("Invalid JWK", ex, args("key", keyObject));
		}
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
			int count = 0;
			for (JWK jwk : jwkSet.getKeys()) {
				var use = jwk.getKeyUse();
				if (use != null && !use.equals(KeyUse.SIGNATURE)) {
					// skip any encryption keys
					continue;
				}
				count++;
			}
			if (count > 1) {
				throw error("The JWKS contains more than one signing key.", args("jwks", jwks));
			}
			if (count > 0) {
				// sign jwt using private key
				SignedJWT jwt = signJWT(jwkSet, claimSet);

				// Verify JWT after signed to check valid JWKs
				verifyJWTAfterSigned(jwkSet, jwt);
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
		} else if (jwk.getKeyType().equals(KeyType.OKP)) {
			OctetKeyPair octetKeyPair = (OctetKeyPair) jwk;
			if(Curve.Ed25519.equals(octetKeyPair.getCurve())) {
				signer = new Ed25519Signer((OctetKeyPair) jwk);
			} else {
				throw error("Unsupported curve for EdDSA alg", args("jwk", jwk.toJSONString()));
			}
		}

		if (signer == null) {
			throw error("Couldn't create signer from key", args("jwk", jwk.toJSONString()));
		}

		Algorithm alg = jwk.getAlgorithm();
		if (alg == null) {
			throw error("keys supplied in the test configuration must contain an 'alg' entry that indicates which algorithm will be used for the test (e.g. 'PS256')", args("jwk", jwk.toJSONString()));
		}

		JWSHeader header = new JWSHeader(JWSAlgorithm.parse(alg.getName()), JOSEObjectType.JWT, null, null, null, null, null, null, null, null, jwk.getKeyID(), true, null, null);

		SignedJWT jwt = new SignedJWT(header, claimSet);

		jwt.sign(signer);
		return jwt;
	}

	private void verifyJWTAfterSigned(JWKSet jwkSet, SignedJWT jwt) throws JOSEException {
		SecurityContext context = new SimpleSecurityContext();

		JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);

		AlternateJWSVerificationKeySelector<SecurityContext> selector = new AlternateJWSVerificationKeySelector<>(jwt.getHeader().getAlgorithm(), jwkSource);

		List<JWK> jwkKeys = selector.selectJWSJwks(jwt.getHeader(), context);
		JWSVerifierFactory factory = new DefaultJWSVerifierFactory();
		for(JWK jwkKey : jwkKeys) {
			JWSVerifier verifier = null;
			try {
				if (jwkKey instanceof OctetKeyPair) {
					OctetKeyPair publicKey = OctetKeyPair.parse(jwkKey.toPublicJWK().toString());
					if (Curve.Ed25519.equals(publicKey.getCurve())) {
						verifier = new Ed25519Verifier(publicKey);
					}
				} else if (jwkKey instanceof AsymmetricJWK asyncJwkKey) {
					KeyPair keyPair = asyncJwkKey.toKeyPair();
					verifier = factory.createJWSVerifier(jwt.getHeader(), keyPair.getPublic());
				} else if (jwkKey instanceof SecretJWK secretJwkKey) {
					verifier = factory.createJWSVerifier(jwt.getHeader(), secretJwkKey.toSecretKey());
				}
			} catch (JOSEException | ParseException e) {
				log("Unable to verifyJWTAfterSigned", args("exception", e));
			}
			if (verifier != null) {
				if (!jwt.verify(verifier)) {
				throw error("Invalid JWKs supplied in configuration. Private and public exponent don't match (test JWS could not be verified)",
					args("jws", jwt.serialize(),
						"jwks", JWKUtil.getPrivateJwksAsJsonObject(jwkSet)));
				}
			}
		}
	}

	private void checkValidStructureInJwks(JsonElement jwks) {
		if (jwks == null) {
			throw error("Couldn't find JWKS in configuration");
		} else if (!(jwks instanceof JsonObject)) {
			throw error("Invalid JWKS (Json Web Key Set) in configuration - it must be a JSON object that contains a 'keys' array.", args("jwks", jwks));
		}

		if (!jwks.getAsJsonObject().has("keys") || !jwks.getAsJsonObject().get("keys").isJsonArray()) {
			throw error("Keys array not found in JWKS", args("jwks", jwks));
		}
	}

	private void verifyKeysIsBase64UrlEncoded(JsonObject keyObject, String... keys) {
		for (String key : keys) {
			String value = OIDFJSON.getString(keyObject.get(key));
			String regex = "[a-zA-Z0-9_-]";
			for (int i = 0; i < value.length(); i++) {
				char character = value.charAt(i);
				if (!Pattern.matches(regex, String.valueOf(character))) {
					throw error("Value of key %s is invalid because it contains the character %s that is not permitted in unpadded base64url".formatted(key, character), args("jwk", keyObject));
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
