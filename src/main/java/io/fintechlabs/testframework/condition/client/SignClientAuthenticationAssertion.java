package io.fintechlabs.testframework.condition.client;

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
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;
import org.apache.http.client.utils.URIBuilder;

import java.net.URISyntaxException;
import java.text.ParseException;

public class SignClientAuthenticationAssertion extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "client_assertion_claims", "client_jwks" })
	@PostEnvironment(strings = "client_assertion")
	public Environment evaluate(Environment env) {
		JsonObject claims = env.getObject("client_assertion_claims");
		JsonObject jwks = env.getObject("client_jwks");

		if (claims == null) {
			throw error("Couldn't find claims");
		}

		if (jwks == null) {
			throw error("Couldn't find jwks");
		}

		try {
			JWTClaimsSet claimSet = JWTClaimsSet.parse(claims.toString());

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
					throw error("No 'alg' field specified in key", args("jwk", jwk.toJSONString()));
				}

				JWSHeader header = new JWSHeader(JWSAlgorithm.parse(alg.getName()), null, null, null, null, null, null, null, null, null, jwk.getKeyID(), null, null);

				SignedJWT assertion = new SignedJWT(header, claimSet);

				assertion.sign(signer);

				final String serializedJwt = assertion.serialize();
				env.putString("client_assertion", serializedJwt);

				String jwtIoUrl;
				JWKSet pubSet = jwkSet.toPublicJWKSet();
				if (pubSet.getKeys().size() == 0) {
					// must be a symmetric key; I can't figure out the right jwt.io url to validate
					jwtIoUrl = null;
				} else {
					try {
						URIBuilder b = new URIBuilder("");
						b.addParameter("token", serializedJwt);
						JWK pubJwk = pubSet.getKeys().iterator().next();
						String keySetString = pubJwk.toString();
						b.addParameter("publicKey", keySetString);

						// We do this odd string append because jwt.io wants something that looks like a url query
						// but it actually part of the fragment: https://jwt.io/#debugger-io?token=...
						jwtIoUrl = "https://jwt.io/#debugger-io" + b.build().toString();
					} catch (URISyntaxException e) {
						throw error("Failed to create URIBuilder");
					}
				}

				logSuccess("Signed the client assertion", args("client_assertion", serializedJwt,
					"jwt_io_link", jwtIoUrl));

				return env;

			} else {
				throw error("Expected only one JWK in the set", args("found", jwkSet.getKeys().size()));
			}

		} catch (ParseException e) {
			throw error(e);
		} catch (JOSEException e) {
			throw error(e);
		}

	}

}
