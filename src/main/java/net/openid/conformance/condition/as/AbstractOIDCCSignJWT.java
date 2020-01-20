package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractSignJWT;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;
import java.util.List;
import java.util.UUID;

public abstract class AbstractOIDCCSignJWT extends AbstractSignJWT {
	public static final Base64URL ALG_NONE_HEADER = Base64URL.encode("{\"alg\":\"none\"}");

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
			String clientSecret = client.get("client_secret").getAsString();
			selectedKey = new OctetSequenceKey.Builder(Base64URL.encode(clientSecret))
								.keyUse(KeyUse.SIGNATURE)
								.algorithm(jwsAlgorithm)
								.build();
		} else {
			try {
				JWKSet jwkSet = JWKSet.parse(jwks.toString());
				if(jwkSet!=null) {
					List<JWK> keys = jwkSet.getKeys();
					selectedKey = selectAsymmetricKey(jwsAlgorithm, keys);
				}
			} catch (ParseException e) {
				throw error("Could not parse server jwks. Failed to find a signing key.", e, args("server_jwks", jwks));
			}
		}
		//throw an error if a key that will satisfy the alg cannot be found
		if(selectedKey == null) {
			throw error("Could not find a signing key", args("signing_algorithm", signingAlg));
		}
		return selectedKey;
	}

	/**
	 * will select the first key with the correct type, use and alg if possible
	 * in the worst case it will select the last key with the correct type
	 * Note: The jwks will contain only 1 matching key since we create it, but just in case...
	 * @param jwsAlgorithm
	 * @param keys
	 * @return null if not found
	 */
	protected JWK selectAsymmetricKey(JWSAlgorithm jwsAlgorithm, List<JWK> keys) {
		JWK selectedKey = null;
		for(JWK key : keys) {
			if(JWSAlgorithm.Family.EC.contains(jwsAlgorithm) && KeyType.EC.equals(key.getKeyType())) {
				if(key.getKeyUse()!=null) {
					if(KeyUse.SIGNATURE.equals(key.getKeyUse()) && JWSAlgorithm.Family.SIGNATURE.contains(key.getAlgorithm())) {
						selectedKey = key;
						break;
					}
				} else {
					selectedKey = key;
				}
			} else if(JWSAlgorithm.Family.ED.contains(jwsAlgorithm) && KeyType.EC.equals(key.getKeyType())) {
				if(key.getKeyUse()!=null) {
					if(KeyUse.SIGNATURE.equals(key.getKeyUse()) && JWSAlgorithm.Family.SIGNATURE.contains(key.getAlgorithm())) {
						selectedKey = key;
						break;
					}
				} else {
					selectedKey = key;
				}
			} else if(jwsAlgorithm.getName().startsWith("PS") && KeyType.RSA.equals(key.getKeyType())) {
				if(key.getKeyUse()!=null) {
					if(KeyUse.SIGNATURE.equals(key.getKeyUse()) && JWSAlgorithm.Family.SIGNATURE.contains(key.getAlgorithm())) {
						selectedKey = key;
						break;
					}
				} else {
					selectedKey = key;
				}
			} else if(jwsAlgorithm.getName().startsWith("RS") && KeyType.RSA.equals(key.getKeyType())) {
				if(key.getKeyUse()!=null) {
					if(KeyUse.SIGNATURE.equals(key.getKeyUse()) && JWSAlgorithm.Family.SIGNATURE.contains(key.getAlgorithm())) {
						selectedKey = key;
						break;
					}
				} else {
					selectedKey = key;
				}
			}
		}
		return selectedKey;
	}

	protected Environment signJWTUsingKey(Environment env, JsonObject claims, JWK jwk) {

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
			} else if (KeyType.OCT.equals(jwk.getKeyType())) {
				signer = new MACSigner((OctetSequenceKey) jwk);
			}

			if (signer == null) {
				throw error("Couldn't create signer from key; kty must be one of 'oct', 'rsa', 'ec'", args("jwk", jwk.toJSONString()));
			}

			Algorithm alg = jwk.getAlgorithm();
			if (alg == null) {
				//unlikely to happen but just in case
				throw error("No 'alg' field specified in key", args("jwk", jwk.toJSONString()));
			}

			JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.parse(alg.getName()))
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
			throw error("Unable to sign; check provided key has correct 'kty' for it's 'alg': " + e.getCause(), e);
		}
	}



}
