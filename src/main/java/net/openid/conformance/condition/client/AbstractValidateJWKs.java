package net.openid.conformance.condition.client;

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
import net.openid.conformance.util.JWKUtil.JwkIssue;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class AbstractValidateJWKs extends AbstractCondition {

	// RFC 8410 object identifiers for the OKP curves (Nimbus's Curve leaves the OID null for these).
	private static final Map<Curve, String> OKP_CURVE_OIDS = Map.of(
		Curve.Ed25519, "1.3.101.112",
		Curve.Ed448, "1.3.101.113",
		Curve.X25519, "1.3.101.110",
		Curve.X448, "1.3.101.111");

	protected void checkJWKs(JsonElement jwks, boolean checkPrivatePart) {

		if (jwks == null) {
			throw error("Couldn't find JWKS in configuration");
		}
		if (!(jwks instanceof JsonObject)) {
			throw error("Invalid JWKS (Json Web Key Set) in configuration - it must be a JSON object that contains a 'keys' array.", args("jwks", jwks));
		}
		JsonObject jwksObject = jwks.getAsJsonObject();
		if (!JWKUtil.hasKeysArray(jwksObject)) {
			throw error("Keys array not found in JWKS", args("jwks", jwks));
		}

		List<JwkIssue> structuralIssues = JWKUtil.findStructurallyInvalidKeys(jwksObject);
		if (!structuralIssues.isEmpty()) {
			JwkIssue first = structuralIssues.get(0);
			throw error("Invalid JWK in JWKS: the key at index " + first.index() + " " + first.detail(),
				args("issues", JWKUtil.issuesToJson(structuralIssues)));
		}

		jwksObject.getAsJsonArray("keys").forEach(keyJsonElement -> {
			JsonObject keyObject = keyJsonElement.getAsJsonObject();
			// Nimbus performs additional checks the structural scan above does not (e.g. x5c bare-key match).
			parseJWKWithNimbus(keyObject);
			if (checkPrivatePart) {
				verifyPrivatePart(jwks, keyObject);
			}
		});
	}

	/**
	 * Nimbusds performs various checks (including the ones manually implemented in this class)
	 * @param keyObject
	 */
	protected void parseJWKWithNimbus(JsonObject keyObject) {
		JWK jwk;
		try {
			//https://openid.net/specs/openid-connect-registration-1_0.html#rfc.section.2
			//jwks
			//    The JWK x5c parameter MAY be used to provide X.509 representations of keys provided.
			//    When used, the bare key values MUST still be present and MUST match those in the certificate
			//Nimbusds enforces this (and other checks like missing properties) for RSA and EC keys while
			//parsing, but NOT for OKP keys: OctetKeyPair.matches() always returns false and its constructor
			//never calls it (see https://bitbucket.org/connect2id/nimbus-jose-jwt/issues/620), so for OKP
			//keys we perform the bare-key-to-certificate check ourselves below.
			jwk = JWK.parse(keyObject.toString());
		} catch (ParseException ex) {
			throw error("Invalid JWK", ex, args("key", keyObject));
		}
		if (jwk instanceof OctetKeyPair okp) {
			ensureOkpKeyMatchesCertificate(okp, keyObject);
		}
	}

	/**
	 * Enforce RFC 7517 section 4.7: when an OKP JWK carries an x5c certificate chain, the bare public
	 * key ("x") MUST match the public key in the leaf certificate. Nimbus performs the equivalent check
	 * for RSA and EC keys during parsing, but {@link OctetKeyPair#matches(X509Certificate)} is a stub
	 * that always returns false, so the check is skipped for OKP keys (e.g. Ed25519). See
	 * <a href="https://bitbucket.org/connect2id/nimbus-jose-jwt/issues/620">nimbus-jose-jwt issue 620</a>.
	 */
	private void ensureOkpKeyMatchesCertificate(OctetKeyPair okp, JsonObject keyObject) {
		List<X509Certificate> chain = okp.getParsedX509CertChain();
		if (chain == null || chain.isEmpty()) {
			return;
		}
		SubjectPublicKeyInfo certKey = SubjectPublicKeyInfo.getInstance(chain.get(0).getPublicKey().getEncoded());
		// An OKP public key is (crv, x), so both the curve (the certificate's algorithm OID) and the bare
		// key octets must match; comparing only "x" would let a same-length key on another curve through.
		String expectedOid = OKP_CURVE_OIDS.get(okp.getCurve());
		boolean curveMismatch = expectedOid != null && !expectedOid.equals(certKey.getAlgorithm().getAlgorithm().getId());
		boolean keyMismatch = !Arrays.equals(certKey.getPublicKeyData().getOctets(), okp.getX().decode());
		if (curveMismatch || keyMismatch) {
			throw error("The JWK supplied in the test configuration has an x5c certificate whose public key "
				+ "does not match the bare key (the 'crv' and 'x' values)", args("key", keyObject));
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
			JWK signingJwk = null;
			for (JWK jwk : jwkSet.getKeys()) {
				var use = jwk.getKeyUse();
				if (use != null && !use.equals(KeyUse.SIGNATURE)) {
					// skip any encryption keys
					continue;
				}
				if (signingJwk == null) {
					signingJwk = jwk;
				}
				count++;
			}
			if (count > 1) {
				throw error("The JWKS contains more than one signing key.", args("jwks", jwks));
			}
			if (count > 0) {
				// sign jwt using private key
				SignedJWT jwt = signJWT(signingJwk, claimSet);

				// Verify JWT after signed to check valid JWKs
				verifyJWTAfterSigned(jwkSet, jwt);
			}
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

	private void verifyKeysIsBase64UrlEncoded(JsonObject keyObject, String... keys) {
		for (String key : keys) {
			String value = OIDFJSON.getString(keyObject.get(key));
			if (!JWKUtil.isBase64Url(value)) {
				throw error("Value of key " + key + " is not valid unpadded base64url", args("jwk", keyObject));
			}
		}
	}
}
