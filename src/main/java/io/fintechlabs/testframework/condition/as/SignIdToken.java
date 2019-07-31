package io.fintechlabs.testframework.condition.as;

import java.text.ParseException;

import com.google.gson.JsonObject;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
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
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class SignIdToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "id_token_claims", "server_jwks" })
	@PostEnvironment(strings = "id_token")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("id_token_claims");
		JsonObject jwks = env.getObject("server_jwks");

		if (claims == null) {
			throw error("Couldn't find claims");
		}

		if (jwks == null) {
			throw error("Couldn't find jwks");
		}

		try {
			JWKSet jwkSet = JWKSet.parse(jwks.toString());

			if (jwkSet.getKeys().size() == 1) {
				// figure out which algorithm to use
				JWK jwk = jwkSet.getKeys().iterator().next();

				JWSSigner signer = null;
				if (jwk.getKeyType().equals(KeyType.RSA)) {
					signer = new RSASSASigner((RSAKey) jwk);
				} else if (jwk.getKeyType().equals(KeyType.EC)) {
					signer = new ECDSASigner((ECKey) jwk);
				} else if (jwk.getKeyType().equals(KeyType.OCT)) {
					signer = new MACSigner((OctetSequenceKey) jwk);
				}

				if (signer == null) {
					throw error("Couldn't create signer from key", args("jwk", jwk.toJSONString()));
				}

				Algorithm alg = jwk.getAlgorithm();
				if (alg == null) {
					throw error("No algorithm specified for key", args("jwk", jwk.toJSONString()));
				}

				JWSHeader header = new JWSHeader(JWSAlgorithm.parse(alg.getName()), JOSEObjectType.JWT, null, null, null, null, null, null, null, null, jwk.getKeyID(), null, null);

				String idToken = performSigning(header, claims, signer);

				env.putString("id_token", idToken);

				String publicKeySetString = (jwk.toPublicJWK() != null ? jwk.toPublicJWK().toString() : null);
				JsonObject verifiableIdTokenObject = new JsonObject();
				verifiableIdTokenObject.addProperty("verifiable_jws", idToken);
				verifiableIdTokenObject.addProperty("public_jwk", publicKeySetString);
				logSuccess("Signed the ID token", args("id_token", verifiableIdTokenObject));

				return env;

			} else {
				throw error("Expected only one JWK in the set. Please ensure the JWKS contains only the signing key to be used.", args("found", jwkSet.getKeys().size()));
			}

		} catch (ParseException e) {
			throw error(e);
		} catch (JOSEException e) {
			throw error(e);
		}

	}

	protected String performSigning(JWSHeader header, JsonObject claims, JWSSigner signer) {

		try {
			JWTClaimsSet claimSet = JWTClaimsSet.parse(claims.toString());

			SignedJWT idToken = new SignedJWT(header, claimSet);

			idToken.sign(signer);

			return idToken.serialize();

		} catch (ParseException e) {
			throw error(e);
		} catch (JOSEException e) {
			throw error(e);
		}

	}

}
