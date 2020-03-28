package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
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
import net.openid.conformance.condition.AbstractCondition;

import java.security.Key;
import java.text.ParseException;
import java.util.List;

public abstract class AbstractVerifyJwsSignature extends AbstractCondition {

	/**
	 *
	 * @param token JWS to be checked
	 * @param publicJwks public keys to use to check the token
	 * @param tokenName String to be used in log messages/keys to describe the item being checked to the user
	 */
	protected void verifyJwsSignature(String token, JsonObject publicJwks, String tokenName) {
		try {
			// translate stored items into nimbus objects
			SignedJWT jwt = SignedJWT.parse(token);
			JWKSet jwkSet = JWKSet.parse(publicJwks.toString());
			JWKSet jwkSetWithKeyValid = null;

			boolean validSignature = false;
			for(JWK jwk: jwkSet.getKeys()) {
				// using each key to verify signature, so that can know the exact key which are able to verify
				if(verifySignature(jwt, new JWKSet(jwk))) {
					jwkSetWithKeyValid = new JWKSet(jwk);
					validSignature = true;
					break;
				}
			}

			if (!validSignature) {
				throw error("Unable to verify "+tokenName+" signature based on server keys", args("jwks", publicJwks, tokenName, token));
			}

			String publicKeySetString = jwkSetWithKeyValid.toPublicJWKSet().getKeys().size() > 0 ? jwkSetWithKeyValid.toPublicJWKSet().getKeys().iterator().next().toString() : null;
			JsonObject tokenObject = new JsonObject();
			tokenObject.addProperty("verifiable_jws", token);
			tokenObject.addProperty("public_jwk", publicKeySetString);
			logSuccess(tokenName + " signature validated", args(tokenName, tokenObject));

		} catch (JOSEException | ParseException e) {
			throw error("Error validating " + tokenName + " signature", e);
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

	protected boolean verifyHMACSignature(SignedJWT jwt, String sharedSecret) throws JOSEException {
		JWSVerifier verifier = new MACVerifier(sharedSecret);
		return jwt.verify(verifier);
	}
}
