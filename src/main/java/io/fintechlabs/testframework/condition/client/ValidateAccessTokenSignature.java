package io.fintechlabs.testframework.condition.client;

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

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class ValidateAccessTokenSignature extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param conditionResultOnFailure
	 * @param requirements
	 */
	public ValidateAccessTokenSignature(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@PreEnvironment(required = { "access_token_jwt", "server_jwks" })
	@Override
	public Environment evaluate(Environment env) {
		if (!env.containsObject("access_token_jwt")) {
			throw error("Couldn't find parsed access token");
		}

		if (!env.containsObject("server_jwks")) {
			throw error("Couldn't find server's public key");
		}

		String accessToken = env.getString("access_token_jwt", "value");
		JsonObject serverJwks = env.getObject("server_jwks"); // to validate the signature

		try {
			// translate stored items into nimbus objects
			SignedJWT jwt = SignedJWT.parse(accessToken);
			JWKSet jwkSet = JWKSet.parse(serverJwks.toString());

			SecurityContext context = new SimpleSecurityContext();

			JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);

			JWSKeySelector<SecurityContext> selector = new JWSVerificationKeySelector<>(jwt.getHeader().getAlgorithm(), jwkSource);

			List<? extends Key> keys = selector.selectJWSKeys(jwt.getHeader(), context);
			for (Key key : keys) {
				JWSVerifierFactory factory = new DefaultJWSVerifierFactory();
				JWSVerifier verifier = factory.createJWSVerifier(jwt.getHeader(), key);

				if (jwt.verify(verifier)) {
					logSuccess("Access token signature validated", args("algorithm", key.getAlgorithm()));
					return env;
				} else {
					// failed to verify with this key, moving on
					// not a failure yet as it might pass a different key
				}
			}

			// if we got here, it hasn't been verified on any key
			throw error("Unable to verify access token signature based on server keys");

		} catch (JOSEException | ParseException e) {
			throw error("Error validating access Token signature", e);
		}
	}

}
