package net.openid.conformance.condition.as;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractVerifyJwsSignature;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

/**
 * token_endpoint_auth_signing_alg
 * ...
 * All Token Requests using these authentication methods from this Client MUST be rejected,
 * if the JWT is not signed with this algorithm
 * ...
 */
public class EnsureClientAssertionSignatureAlgorithmMatchesRegistered extends AbstractVerifyJwsSignature {

	@Override
	@PreEnvironment(required = { "client", "client_assertion" })
	public Environment evaluate(Environment env) {

		String clientAssertionString = env.getString("client_assertion", "value");
		try {
			SignedJWT jwt = SignedJWT.parse(clientAssertionString);
			String expectedAlgName = env.getString("client", "token_endpoint_auth_signing_alg");
			if(expectedAlgName!=null) {
				JWSAlgorithm expectedAlg = JWSAlgorithm.parse(expectedAlgName);
				JWSAlgorithm actualAlg = jwt.getHeader().getAlgorithm();
				if(expectedAlg.equals(actualAlg)) {
					logSuccess("Client assertion is signed using the registered token_endpoint_auth_signing_alg algorithm",
								args("algorithm", expectedAlgName));
				} else {
					throw error("Client assertion is not signed using the registered token_endpoint_auth_signing_alg algorithm.",
						args("expected", expectedAlgName, "actual", actualAlg.getName()));
				}
			} else {
				log("token_endpoint_auth_signing_alg is not set for the client, any supported algorithm can be used");
			}
		} catch (ParseException ex) {
			throw error("Invalid client assertion", ex,
				args("client_assertion", clientAssertionString));
		}
		return env;
	}
}
