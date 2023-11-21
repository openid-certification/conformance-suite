package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
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
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

public class ForceIdTokenToBeSignedWithRS256 extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "id_token", required = "server_jwks")
	@PostEnvironment(strings = "id_token")
	public Environment evaluate(Environment env) {

		String idTokenString = env.getString("id_token");

		JsonObject jwks = env.getObject("server_jwks");

		try {
			SignedJWT idToken = SignedJWT.parse(idTokenString);

			JWSAlgorithm alg = idToken.getHeader().getAlgorithm();

			if (!alg.equals(JWSAlgorithm.PS256)) {
				logSuccess("Server key in the provided configuration uses alg " + alg + ", so this test can't be performed; continuing using a normal correctly signed id_token instead");
				return env;
			}

			alg = JWSAlgorithm.RS256;

			//Rebuild new header with alternated algorithm
			JWSHeader header = new JWSHeader(JWSAlgorithm.parse(alg.getName()), JOSEObjectType.JWT, null, null, null, null, null, null, null, null, idToken.getHeader().getKeyID(), true, null, null);

			//Rebuild new token
			JWKSet jwkSet = JWKSet.parse(jwks.toString());

			JWK jwk = jwkSet.getKeys().iterator().next();

			JWSSigner signer = null;
			if (jwk.getKeyType().equals(KeyType.RSA)) {
				signer = new RSASSASigner((RSAKey) jwk);
			} else if (jwk.getKeyType().equals(KeyType.EC)) {
				signer = new ECDSASigner((ECKey) jwk);
			} else if (jwk.getKeyType().equals(KeyType.OCT)) {
				signer = new MACSigner((OctetSequenceKey) jwk);
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
