package net.openid.conformance.vci10issuer.condition;

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
import com.nimbusds.jose.proc.JWSVerifierFactory;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.security.KeyPair;
import java.text.ParseException;
import java.util.regex.Pattern;

/**
 * VCI-specific validation for client JWKS that allows multiple signing keys.
 * This is needed because VCI with attestation proof type can have multiple keys
 * in the client JWKS that will be put into the attested_keys claim, and the
 * issuer should issue a credential for each key per VCI spec F.1 and F.3.
 */
public class VCIValidateClientJWKsPrivatePart extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client")
	public Environment evaluate(Environment env) {
		JsonElement jwks = env.getElementFromObject("client", "jwks");

		checkJWKs(jwks);

		logSuccess("Valid client JWKs for VCI: keys are valid JSON, contain the required fields, " +
			"the private/public exponents match and are correctly encoded using unpadded base64url. " +
			"Multiple signing keys are allowed for VCI attestation proof type.");

		return env;
	}

	protected void checkJWKs(JsonElement jwks) {
		checkValidStructureInJwks(jwks);

		JsonArray jwksKeyArray = jwks.getAsJsonObject().getAsJsonArray("keys");
		jwksKeyArray.forEach(keyJsonElement -> {
			JsonObject keyObject = keyJsonElement.getAsJsonObject();

			checkMissingKey(keyObject, "kty");
			String kty = OIDFJSON.getString(keyObject.getAsJsonPrimitive("kty"));

			if ("RSA".equals(kty)) {
				checkMissingKey(keyObject, "e", "n");
				verifyKeysIsBase64UrlEncoded(keyObject, "e", "n");
				verifyPrivatePart(keyObject);
			} else if ("EC".equals(kty)) {
				checkMissingKey(keyObject, "x", "y");
				verifyKeysIsBase64UrlEncoded(keyObject, "x", "y");
				verifyPrivatePart(keyObject);
			} else if ("OKP".equals(kty)) {
				checkMissingKey(keyObject, "x", "crv");
				String crv = OIDFJSON.getString(keyObject.getAsJsonPrimitive("crv"));
				if (!Curve.Ed25519.getName().equals(crv)) {
					log("Jwks contains an unsupported curve", args("jwks", keyJsonElement));
				}
				verifyKeysIsBase64UrlEncoded(keyObject, "x");
				verifyPrivatePart(keyObject);
			}
			parseJWKWithNimbus(keyObject);
		});

		// Validate each key individually (sign and verify)
		validateAllKeys(jwks);
	}

	protected void parseJWKWithNimbus(JsonObject keyObject) {
		try {
			@SuppressWarnings("unused")
			JWK jwk = JWK.parse(keyObject.toString());
		} catch (ParseException ex) {
			throw error("Invalid JWK", ex, args("key", keyObject));
		}
	}

	private void verifyPrivatePart(JsonObject keyObject) {
		if (!keyObject.has("d")) {
			throw error("The JWK supplied in the configuration seems to be a public key " +
				"(the 'd' key is missing). You must supply a private key in the test configuration.",
				args("jwk", keyObject));
		}
		verifyKeysIsBase64UrlEncoded(keyObject, "d");
	}

	/**
	 * Validates all signing keys in the JWKS by signing and verifying a test JWT with each.
	 * Unlike the standard validation, this allows multiple signing keys for VCI use cases.
	 */
	private void validateAllKeys(JsonElement jwks) {
		JsonObject claimObject = new JsonObject();
		claimObject.addProperty("test", "does private/public exponent match?");

		try {
			JWTClaimsSet claimSet = JWTClaimsSet.parse(claimObject.toString());
			JWKSet jwkSet = JWKSet.parse(jwks.toString());

			int signingKeyCount = 0;
			for (JWK jwk : jwkSet.getKeys()) {
				var use = jwk.getKeyUse();
				if (use != null && !use.equals(KeyUse.SIGNATURE)) {
					// skip any encryption keys
					continue;
				}
				signingKeyCount++;

				// Validate this individual key by signing and verifying
				SignedJWT jwt = signJWT(jwk, claimSet);
				verifyJWT(jwk, jwt);
			}

			if (signingKeyCount == 0) {
				throw error("No signing keys found in JWKS", args("jwks", jwks));
			}

			log("Validated " + signingKeyCount + " signing key(s) in client JWKS",
				args("signing_key_count", signingKeyCount));

		} catch (JOSEException | ParseException e) {
			throw error("Error validating JWKS", ex(e, args("jwks", jwks)));
		}
	}

	private SignedJWT signJWT(JWK jwk, JWTClaimsSet claimSet) throws JOSEException {
		JWSSigner signer = null;
		if (jwk.getKeyType().equals(KeyType.RSA)) {
			signer = new RSASSASigner((RSAKey) jwk);
		} else if (jwk.getKeyType().equals(KeyType.EC)) {
			signer = new ECDSASigner((ECKey) jwk);
		} else if (jwk.getKeyType().equals(KeyType.OKP)) {
			OctetKeyPair octetKeyPair = (OctetKeyPair) jwk;
			if (Curve.Ed25519.equals(octetKeyPair.getCurve())) {
				signer = new Ed25519Signer(octetKeyPair);
			} else {
				throw error("Unsupported curve for EdDSA alg", args("jwk", jwk.toJSONString()));
			}
		}

		if (signer == null) {
			throw error("Couldn't create signer from key", args("jwk", jwk.toJSONString()));
		}

		Algorithm alg = jwk.getAlgorithm();
		if (alg == null) {
			throw error("Keys supplied in the test configuration must contain an 'alg' entry " +
				"that indicates which algorithm will be used for the test (e.g. 'PS256')",
				args("jwk", jwk.toJSONString()));
		}

		JWSHeader header = new JWSHeader(JWSAlgorithm.parse(alg.getName()), JOSEObjectType.JWT,
			null, null, null, null, null, null, null, null, jwk.getKeyID(), true, null, null);

		SignedJWT jwt = new SignedJWT(header, claimSet);
		jwt.sign(signer);
		return jwt;
	}

	private void verifyJWT(JWK jwk, SignedJWT jwt) throws JOSEException {
		JWSVerifierFactory factory = new DefaultJWSVerifierFactory();
		JWSVerifier verifier = null;

		try {
			if (jwk instanceof OctetKeyPair octetKeyPair) {
				OctetKeyPair publicKey = OctetKeyPair.parse(octetKeyPair.toPublicJWK().toString());
				if (Curve.Ed25519.equals(publicKey.getCurve())) {
					verifier = new Ed25519Verifier(publicKey);
				}
			} else if (jwk instanceof AsymmetricJWK asymmetricJwk) {
				KeyPair keyPair = asymmetricJwk.toKeyPair();
				verifier = factory.createJWSVerifier(jwt.getHeader(), keyPair.getPublic());
			} else if (jwk instanceof SecretJWK secretJwk) {
				verifier = factory.createJWSVerifier(jwt.getHeader(), secretJwk.toSecretKey());
			}
		} catch (JOSEException | ParseException e) {
			throw error("Unable to create verifier for key", e, args("jwk", jwk.toJSONString()));
		}

		if (verifier != null && !jwt.verify(verifier)) {
			throw error("Invalid JWK supplied in configuration. Private and public exponent don't match " +
				"(test JWS could not be verified)",
				args("jws", jwt.serialize(), "jwk", jwk.toPublicJWK().toJSONString()));
		}
	}

	private void checkValidStructureInJwks(JsonElement jwks) {
		if (jwks == null) {
			throw error("Couldn't find JWKS in configuration");
		} else if (!(jwks instanceof JsonObject)) {
			throw error("Invalid JWKS (Json Web Key Set) in configuration - " +
				"it must be a JSON object that contains a 'keys' array.", args("jwks", jwks));
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
					throw error(("Value of key %s is invalid because it contains the character %s " +
						"that is not permitted in unpadded base64url").formatted(key, character),
						args("jwk", keyObject));
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
