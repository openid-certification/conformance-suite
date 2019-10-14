package net.openid.conformance.condition.as;

import java.security.Key;
import java.text.ParseException;
import java.util.List;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
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
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateResourceAssertionSignature extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"resource_assertion", "resource_public_jwks"})
	public Environment evaluate(Environment env) {

		if (!env.containsObject("resource_assertion")) {
			throw error("Couldn't find assertion");
		}

		if (!env.containsObject("resource_public_jwks")) {
			throw error("Couldn't find resource's public key");
		}

		String assertion = env.getString("resource_assertion", "assertion");
		JsonObject serverJwks = env.getObject("resource_public_jwks"); // to validate the signature

		try {
			// translate stored items into nimbus objects
			SignedJWT jwt = SignedJWT.parse(assertion);
			JWKSet jwkSet = JWKSet.parse(serverJwks.toString());

			SecurityContext context = new SimpleSecurityContext();

			JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);

			JWSKeySelector<SecurityContext> selector = new JWSVerificationKeySelector<>(jwt.getHeader().getAlgorithm(), jwkSource);

			List<? extends Key> keys = selector.selectJWSKeys(jwt.getHeader(), context);
			for (Key key : keys) {
				JWSVerifierFactory factory = new DefaultJWSVerifierFactory();
				JWSVerifier verifier = factory.createJWSVerifier(jwt.getHeader(), key);

				if (jwt.verify(verifier)) {
					logSuccess("ID Token signature validated", args("algorithm", key.getAlgorithm()));
					return env;
				} else {
					// failed to verify with this key, moving on
					// not a failure yet as it might pass a different key
				}
			}

			// if we got here, it hasn't been verified on any key
			throw error("Unable to verify assertion signature based on resource keys");

		} catch (JOSEException | ParseException e) {
			throw error("Error validating assertion signature", e);
		}

	}

}
