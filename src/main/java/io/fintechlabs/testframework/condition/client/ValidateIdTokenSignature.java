package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.JWSVerifierFactory;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.proc.SimpleSecurityContext;
import com.nimbusds.jwt.SignedJWT;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

import java.security.Key;
import java.text.ParseException;
import java.util.List;

public class ValidateIdTokenSignature extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "id_token", "server_jwks" })
	public Environment evaluate(Environment env) {

		if (!env.containsObject("id_token")) {
			throw error("Couldn't find parsed ID token");
		}

		if (!env.containsObject("server_jwks")) {
			throw error("Couldn't find server's public key");
		}

		String idToken = env.getString("id_token", "value");
		JsonObject serverJwks = env.getObject("server_jwks"); // to validate the signature

		try {
			// translate stored items into nimbus objects
			SignedJWT jwt = SignedJWT.parse(idToken);
			JWKSet jwkSet = JWKSet.parse(serverJwks.toString());
			JWSHeader header = jwt.getHeader();

			// if a kid is given
			// FIXME: this check is temporarily disabled due to https://gitlab.com/openid/conformance-suite/issues/580
			if (false) /* header != null && !Strings.isNullOrEmpty(header.getKeyID())) */ {
				JWK key = jwkSet.getKeyByKeyId(header.getKeyID());

				if (key == null) {
					throw error("Couldn't find key by the 'kid' property from the header of the id_token", args("jwks", serverJwks, "kid", header.getKeyID(), "id_token", idToken));
				}

				jwkSet = new JWKSet(key);

				if (!verifySignature(jwt, jwkSet)) {
					throw error("Unable to verify ID token signature based on server key that found by the 'kid' property", args("jwks", serverJwks, "kid", header.getKeyID(), "id_token", idToken));
				}
			} else {
				// if a kid isn't given
				boolean validSignature = false;
				for(JWK jwk: jwkSet.getKeys()) {
					// using each key to verify signature, so that can know the exact key which are able to verify
					jwkSet = new JWKSet(jwk);
					if(verifySignature(jwt, jwkSet)) {
						validSignature = true;
						break;
					}
				}
				if (!validSignature) {
					throw error("Unable to verify ID token signature based on server keys", args("jwks", serverJwks, "id_token", idToken));
				}
			}

			String publicKeySetString = jwkSet.toPublicJWKSet().getKeys().size() > 0 ? jwkSet.toPublicJWKSet().getKeys().iterator().next().toString() : null;
			JsonObject idTokenObject = new JsonObject();
			idTokenObject.addProperty("verifiable_jws", idToken);
			idTokenObject.addProperty("public_jwk", publicKeySetString);
			logSuccess("ID Token signature validated", args("id_token", idTokenObject));

			return env;

		} catch (JOSEException | ParseException e) {
			throw error("Error validating ID Token signature", e);
		}

	}

	protected boolean verifySignature(SignedJWT jwt, JWKSet jwkSet) throws JOSEException {
		SecurityContext context = new SimpleSecurityContext();

		JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);

		JWSKeySelector<SecurityContext> selector = new JWSVerificationKeySelector<>(jwt.getHeader().getAlgorithm(), jwkSource);

		List<? extends Key> keys = selector.selectJWSKeys(jwt.getHeader(), context);
		for (Key key : keys) {
			JWSVerifierFactory factory = new DefaultJWSVerifierFactory();
			JWSVerifier verifier = factory.createJWSVerifier(jwt.getHeader(), key);

			if (jwt.verify(verifier)) {
				return true;
			} else {
				// failed to verify with this key, moving on
				// not a failure yet as it might pass a different key
			}
		}
		// if we got here, it hasn't been verified on any key
		return false;
	}

}
