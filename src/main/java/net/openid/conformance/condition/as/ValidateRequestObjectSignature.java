package net.openid.conformance.condition.as;

import java.security.Key;
import java.text.ParseException;
import java.util.List;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
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
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateRequestObjectSignature extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "authorization_request_object", "client_public_jwks", "client" })
	@PostEnvironment(strings = "request_object_signing_alg")
	public Environment evaluate(Environment env) {

		String requestObject = env.getString("authorization_request_object", "value");
		JsonObject clientJwks = env.getObject("client_public_jwks");

		try {

			SignedJWT jwt = SignedJWT.parse(requestObject);
			JWKSet jwkSet = JWKSet.parse(clientJwks.toString());

			JsonObject client = env.getObject("client");
			if(client.has("request_object_signing_alg")) {
				//https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata
				//request_object_signing_alg
				//All Request Objects from this Client MUST be rejected, if not signed with this algorithm.
				//The default, if omitted, is that any algorithm supported by the OP and the RP MAY be used
				String expectedAlg = OIDFJSON.getString(client.get("request_object_signing_alg"));
				JWSAlgorithm jwsAlgorithm = jwt.getHeader().getAlgorithm();
				if(!jwsAlgorithm.getName().equals(expectedAlg)) {
					throw error("Algorithm in JWT header does not match client request_object_signing_alg.",
						args("actual", jwsAlgorithm.getName(), "expected", expectedAlg));
				}
			}

			SecurityContext context = new SimpleSecurityContext();

			JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);

			JWSKeySelector<SecurityContext> selector = new JWSVerificationKeySelector<>(jwt.getHeader().getAlgorithm(), jwkSource);
			//TODO signature verification should be changed to use kids
			List<? extends Key> keys = selector.selectJWSKeys(jwt.getHeader(), context);
			for (Key key : keys) {
				JWSVerifierFactory factory = new DefaultJWSVerifierFactory();
				JWSVerifier verifier = factory.createJWSVerifier(jwt.getHeader(), key);

				if (jwt.verify(verifier)) {
					String alg = jwt.getHeader().getAlgorithm().getName();
					env.putString("request_object_signing_alg", alg);
					logSuccess("Request object signature validated using a key in the client's JWKS " +
										"and using the client's registered request_object_signing_alg",
									args("request_object_signing_alg", alg,
											"jwk", key.toString(), "request_object", requestObject));
					return env;
				} else {
					// failed to verify with this key, moving on
					// not a failure yet as it might pass a different key
				}
			}

			// if we got here, it hasn't been verified by any key
			throw error("Unable to verify request object signature based on client keys");

		} catch (JOSEException | ParseException e) {
			throw error("error validating request object signature", e);
		}

	}

}
