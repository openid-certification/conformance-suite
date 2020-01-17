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
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;
import java.util.List;
import java.util.UUID;

public class OIDCCSignIdToken extends AbstractSignJWT {

	@Override
	@PreEnvironment(required = { "id_token_claims", "server_jwks", "client" }, strings = "signing_algorithm")
	@PostEnvironment(strings = "id_token")
	public Environment evaluate(Environment env) {
		JsonObject claims = env.getObject("id_token_claims");
		JsonObject jwks = env.getObject("server_jwks");
		String signingAlg = env.getString("signing_algorithm");

		JWK selectedKey = null;
		if(signingAlg.startsWith("HS")) {
			//if using MAC based alg, create a jwk from client secret
			String clientSecret = env.getString("client", "client_secret");
			selectedKey = new OctetSequenceKey(Base64URL.encode(clientSecret), KeyUse.SIGNATURE, null, JWSAlgorithm.parse(signingAlg), UUID.randomUUID().toString(), null, null, null, null, null);
		} else {
			try {
				JWKSet jwkSet = JWKSet.parse(jwks.toString());
				if(jwkSet!=null) {
					List<JWK> keys = jwkSet.getKeys();
					for(JWK key : keys) {
						if(signingAlg.startsWith("ES") && key.getKeyType().equals(KeyType.EC)
							&& key.getKeyUse().equals(KeyUse.SIGNATURE) && key.getAlgorithm().getName().startsWith("ES")) {
							selectedKey = key;
							break;
						} else if(signingAlg.startsWith("Ed") && key.getKeyType().equals(KeyType.EC)
							&& key.getKeyUse().equals(KeyUse.SIGNATURE) && key.getAlgorithm().getName().startsWith("Ed")) {
							selectedKey = key;
							break;
						} else if(signingAlg.startsWith("PS") && key.getKeyType().equals(KeyType.RSA)
							&& key.getKeyUse().equals(KeyUse.SIGNATURE) && key.getAlgorithm().getName().startsWith("PS")) {
							selectedKey = key;
							break;
						} else if(signingAlg.startsWith("RS") && key.getKeyType().equals(KeyType.RSA)
							&& key.getKeyUse().equals(KeyUse.SIGNATURE) && key.getAlgorithm().getName().startsWith("RS")) {
							selectedKey = key;
							break;
						}
					}
				}
			} catch (ParseException e) {
				throw error("Could not parse server jwks. Failed to find a signing key.", e, args("server_jwks", jwks));
			}
		}
		//throw an error if a key that will satisfy client id_token_signed_response_alg cannot be found
		if(selectedKey == null) {
			throw error("Could not find an id token signing key", args("signing_algorithm", signingAlg));
		}
		return signJWTUsingKey(env, claims, selectedKey);
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
			if (jwk.getKeyType().equals(KeyType.RSA)) {
				signer = new RSASSASigner((RSAKey) jwk);
			} else if (jwk.getKeyType().equals(KeyType.EC)) {
				signer = new ECDSASigner((ECKey) jwk);
			} else if (jwk.getKeyType().equals(KeyType.OCT)) {
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

			JWSHeader header = new JWSHeader(JWSAlgorithm.parse(alg.getName()), null, null, null, null, null, null, null, null, null, jwk.getKeyID(), null, null);

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
			throw error("Unable to sign ID token; check provided key has correct 'kty' for it's 'alg': " + e.getCause(), e);
		}
	}

	@Override
	protected void logSuccessByJWTType(Environment env, JWTClaimsSet claimSet, JWK jwk, JWSHeader header, String jws, JsonObject verifiableObj) {
		env.putString("id_token", jws);
		logSuccess("Signed the ID token", args("id_token", verifiableObj));
	}

}
