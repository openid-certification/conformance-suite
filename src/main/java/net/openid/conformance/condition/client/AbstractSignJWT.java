package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.Ed25519Signer;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWKUtil;

import java.text.ParseException;
import java.util.List;

public abstract class AbstractSignJWT extends AbstractCondition {
	public static final Base64URL ALG_NONE_HEADER = Base64URL.encode("{\"alg\":\"none\"}");

	protected Environment signJWT(Environment env, JsonObject claims, JsonObject jwks) {
		return signJWT(env, claims, jwks, false);
	}

	/**
	 * Expects only one non-encryption JWK in jwks
	 */
	protected Environment signJWT(Environment env, JsonObject claims, JsonObject jwks, boolean includeTyp) {

		if (claims == null) {
			throw error("Couldn't find claims");
		}

		if (jwks == null) {
			throw error("Couldn't find jwks");
		}

		try {
			JWTClaimsSet claimSet = JWTClaimsSet.parse(claims.toString());
			int count = 0;
			JWK signingJwk = null;

			JWKSet jwkSet = JWKSet.parse(jwks.toString());

			for (JWK jwk : jwkSet.getKeys()) {
				var use = jwk.getKeyUse();
				if (use != null && !use.equals(KeyUse.SIGNATURE)) {
					// skip any encryption keys
					continue;
				}
				count++;
				signingJwk = jwk;
			}

			if (count == 0) {
				throw error("Did not find a key with 'use': 'sig' or no 'use' claim, no key available to sign jwt", args("jwks", jwks));
			}
			if (count > 1) {
				throw error("Expected only one signing JWK in the set. Please ensure the signing key is the only one in the jwks, or that other keys have a 'use' other than 'sig'.", args("jwks", jwks));
			}

			JWSSigner signer = null;
			if (signingJwk.getKeyType().equals(KeyType.RSA)) {
				signer = new RSASSASigner((RSAKey) signingJwk);
			} else if (signingJwk.getKeyType().equals(KeyType.EC)) {
				signer = new ECDSASigner((ECKey) signingJwk);
			} else if (signingJwk.getKeyType().equals(KeyType.OCT)) {
				signer = new MACSigner((OctetSequenceKey) signingJwk);
			}

			if (signer == null) {
				throw error("Couldn't create signer from key; kty must be one of 'oct', 'rsa', 'ec'", args("jwk", signingJwk.toJSONString()));
			}

			Algorithm alg = signingJwk.getAlgorithm();
			if (alg == null) {
				throw error("No 'alg' field specified in key; please add 'alg' field in the configuration", args("jwk", signingJwk.toJSONString()));
			}

			JWSHeader header = new JWSHeader(JWSAlgorithm.parse(alg.getName()),
				includeTyp ? JOSEObjectType.JWT : null,
				null, null, null, null, null, null, null, null,
				signingJwk.getKeyID(), null, null);

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
