package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

public class ForceIdTokenToBeSignedWithAltRS256 extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "id_token", required = "server_alt_jwks")
	@PostEnvironment(strings = "id_token")
	public Environment evaluate(Environment env) {

		String idTokenString = env.getString("id_token");

		JsonObject jwks = env.getObject("server_alt_jwks");

		try {
			SignedJWT idToken = SignedJWT.parse(idTokenString);

			//Rebuild new token
			JWKSet jwkSet = JWKSet.parse(jwks.toString());

			JWK jwk = jwkSet.getKeys().iterator().next();

			JWSAlgorithm alg = JWSAlgorithm.RS256;

			//Rebuild new header with alternated algorithm
			JWSHeader header = new JWSHeader(JWSAlgorithm.parse(alg.getName()), JOSEObjectType.JWT, null, null, null, null, null, null, null, null, jwk.getKeyID(), true, null, null);

			JWSSigner signer = null;
			if (jwk.getKeyType().equals(KeyType.RSA)) {
				signer = new RSASSASigner((RSAKey) jwk);
			} else {
				throw error("Invalid key type for RS256 signature.");
			}

			SignedJWT invalidIdToken = new SignedJWT(header, idToken.getJWTClaimsSet());

			invalidIdToken.sign(signer);

			env.putString("id_token", invalidIdToken.serialize());

			logSuccess("Signed the ID token with alg of RS256", args("id_token_header", invalidIdToken.getHeader(), "id_token_claims", invalidIdToken.getJWTClaimsSet(), "id_token serialized", invalidIdToken.serialize()));

			return env;

		} catch (ParseException e) {
			throw error("Couldn't parse JWT", e, args("id_token", idTokenString));
		}  catch (JOSEException e) {
			throw error(e);
		}

	}

}
