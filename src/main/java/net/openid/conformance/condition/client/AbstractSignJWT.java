package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.Ed25519Signer;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.produce.JWSSignerFactory;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.extensions.MultiJWSSignerFactory;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWKUtil;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

public abstract class AbstractSignJWT extends AbstractGetSigningKey {
	public static final Base64URL ALG_NONE_HEADER = Base64URL.encode("{\"alg\":\"none\"}");

	protected Environment signJWT(Environment env, JsonObject claims, JsonObject jwks) {
		return signJWT(env, claims, jwks, false);
	}

	protected Environment signJWT(Environment env, JsonObject claims, JsonObject jwks, boolean includeTyp) {
		return signJWT(env, claims, jwks, includeTyp, false, false, false);
	}

	protected JOSEObjectType getMediaType() {
		return JOSEObjectType.JWT;
	}

	/**
	 * Expects only one non-encryption JWK in jwks
	 */
	protected Environment signJWT(Environment env, JsonObject claims, JsonObject jwks, boolean includeTyp, boolean includeX5tS256, boolean includeX5c, boolean errorIfX5cMissing) {

		if (claims == null) {
			throw error("Couldn't find claims");
		}

		if (jwks == null) {
			throw error("Couldn't find jwks");
		}

		try {
			JWTClaimsSet claimSet = JWTClaimsSet.parse(claims.toString());
			JWK signingJwk = getSigningKey("signing", jwks);
			Algorithm algorithm = signingJwk.getAlgorithm();
			if (algorithm == null) {
				throw error("No 'alg' field specified in key; please add 'alg' field in the configuration", args("jwk", signingJwk));
			}
			JWSAlgorithm alg = JWSAlgorithm.parse(algorithm.getName());

			JWSSignerFactory jwsSignerFactory = MultiJWSSignerFactory.getInstance();
			JWSSigner signer = jwsSignerFactory.createJWSSigner(signingJwk, alg);

			JWSHeader.Builder builder = new JWSHeader.Builder(alg);
			if (includeTyp) {
				builder.type(getMediaType());
			}
			if (includeX5tS256) {
				builder.x509CertSHA256Thumbprint(signingJwk.computeThumbprint());
			}
			if (includeX5c) {
				if (signingJwk.getX509CertChain() == null) {
					if (errorIfX5cMissing) {
						throw error("A x5c entry is required in the client's signing key but isn't present in the configuration", args("clientjwks", jwks));
					}
				} else {
					builder.x509CertChain(signingJwk.getX509CertChain());
				}
			}
			builder.keyID(signingJwk.getKeyID());
			JWSHeader header = builder.build();

			String jws = performSigning(header, claims, signer);

			String publicKeySetString = (signingJwk.toPublicJWK() != null ? signingJwk.toPublicJWK().toString() : null);
			JsonObject verifiableObj = new JsonObject();
			verifiableObj.addProperty("verifiable_jws", jws);
			verifiableObj.addProperty("public_jwk", publicKeySetString);

			logSuccessByJWTType(env, claimSet, signingJwk, header, jws, verifiableObj);

			return env;

		} catch (ParseException e) {
			throw error(e);
		} catch (JOSEException e) {
			throw error("Unable to sign client assertion; check provided key has correct 'kty' for it's 'alg': " + e.getCause(), e);
		}
	}

	protected String performSigning(JWSHeader header, JsonObject claims, JWSSigner signer) throws JOSEException, ParseException {
		JWTClaimsSet claimSet = JWTClaimsSet.parse(claims.toString());

		SignedJWT signJWT = new SignedJWT(header, claimSet);

		signJWT.sign(signer);

		return signJWT.serialize();
	}

	protected String performSigningEnsureAudIsArray(JWSHeader header, JsonObject claims, JWSSigner signer) throws JOSEException, ParseException {
		class SignedJWTKeepAudArray extends JWSObject {
			private static final long serialVersionUID = 1L;

			public SignedJWTKeepAudArray(JWSHeader header, Payload payload) {
				super(header, payload);
			}
		}

		JWTClaimsSet claimSet = JWTClaimsSet.parse(claims.toString());
		Map<String, Object> payloadJson = claimSet.toJSONObject();

		/*
		 * The default behaviour of JWTClaimsSet.toJSONObject() is to convert a single element 'aud' claim array to a string.
		 *
		 * Here we ensure it remains an array.
		 */
		Object audClaim = claims.get("aud");
		if (audClaim != null && audClaim instanceof JsonArray) {
			if (payloadJson.get("aud") instanceof String) {
				List<Object> audList = List.<Object>of(payloadJson.get("aud"));
				payloadJson.put("aud", audList);
			}
		}

		SignedJWTKeepAudArray signJWT = new SignedJWTKeepAudArray(header, new Payload(payloadJson));

		signJWT.sign(signer);

		return signJWT.serialize();
	}

	protected abstract void logSuccessByJWTType(Environment env, JWTClaimsSet claimSet, JWK jwk, JWSHeader header, String jws, JsonObject verifiableObj);


	protected String signWithAlgNone(String input) {
		String jwt =  ALG_NONE_HEADER + "." + Base64URL.encode(input) + ".";
		return jwt;
	}

	/**
	 * Creates an OctetSequenceKey is using a symmetric alg and the client secret
	 * or
	 * Selects a key from the jwks
	 * @param jwks
	 * @param signingAlg
	 * @param client
	 * @return
	 */
	public JWK selectOrCreateKey(JsonObject jwks, String signingAlg, JsonObject client) {

		JWSAlgorithm jwsAlgorithm = JWSAlgorithm.parse(signingAlg);
		JWK selectedKey = null;
		if(JWSAlgorithm.Family.HMAC_SHA.contains(jwsAlgorithm)) {
			//if using MAC based alg, create a jwk from client secret
			String clientSecret = OIDFJSON.getString(client.get("client_secret"));
			selectedKey = new OctetSequenceKey.Builder(Base64URL.encode(clientSecret))
				.keyUse(KeyUse.SIGNATURE)
				.algorithm(jwsAlgorithm)
				.build();
		} else {
			try {
				JWKSet jwkSet = JWKSet.parse(jwks.toString());
				if(jwkSet!=null) {
					List<JWK> keys = jwkSet.getKeys();
					selectedKey = JWKUtil.selectAsymmetricJWSKey(jwsAlgorithm, keys);
				}
			} catch (ParseException e) {
				throw error("Could not parse jwks. Failed to find a signing key.", e,
							args("jwks", jwks, "alg", signingAlg));
			}
			//throw an error if a key that will satisfy the alg cannot be found
			if(selectedKey == null) {
				throw error("Jwks does not contain a suitable signing key for the selected algorithm",
							args("signing_algorithm", signingAlg));
			}
		}
		return selectedKey;
	}



	protected Environment signJWTUsingKey(Environment env, JsonObject claims, JWK jwk, String alg) {

		if (claims == null) {
			throw error("Couldn't find claims");
		}

		if (jwk == null) {
			throw error("A JWK is required for signing");
		}

		try {
			JWTClaimsSet claimSet = JWTClaimsSet.parse(claims.toString());
			JWSSigner signer = null;

			if (KeyType.RSA.equals(jwk.getKeyType())) {
				signer = new RSASSASigner((RSAKey) jwk);
			} else if (KeyType.EC.equals(jwk.getKeyType())) {
				signer = new ECDSASigner((ECKey) jwk);
				signer.getJCAContext().setProvider(BouncyCastleProviderSingleton.getInstance());
			} else if (KeyType.OCT.equals(jwk.getKeyType())) {
				signer = new MACSigner((OctetSequenceKey) jwk);
			} else if (KeyType.OKP.equals(jwk.getKeyType())) {
				signer = new Ed25519Signer((OctetKeyPair) jwk);
			}

			if (signer == null) {
				throw error("Couldn't create signer from key; kty must be one of 'oct', 'rsa', 'ec'", args("jwk", jwk.toJSONString()));
			}


			JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.parse(alg))
				.keyID(jwk.getKeyID())
				.build();

			String jws = performSigning(header, claims, signer);

			String publicKeySetString = (jwk.toPublicJWK() != null ? jwk.toPublicJWK().toString() : null);
			JsonObject verifiableObj = new JsonObject();
			verifiableObj.addProperty("verifiable_jws", jws);
			verifiableObj.addProperty("public_jwk", publicKeySetString);

			logSuccessByJWTType(env, claimSet, jwk, header, jws, verifiableObj);

			return env;

		} catch (ParseException e) {
			throw error(e);
		} catch (JOSEException e) {
			String message = e.getMessage();
			if(e.getCause()!=null) {
				message = message + " (" + e.getCause().getMessage() + ")";
			}
			throw error("Unable to sign: " + message, e);
		}
	}
}
