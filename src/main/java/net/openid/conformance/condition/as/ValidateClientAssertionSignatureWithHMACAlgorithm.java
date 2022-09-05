package net.openid.conformance.condition.as;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractVerifyJwsSignature;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

public class ValidateClientAssertionSignatureWithHMACAlgorithm extends AbstractVerifyJwsSignature {

	@Override
	@PreEnvironment(required = { "client", "client_assertion" })
	public Environment evaluate(Environment env) {

		String clientAssertionString = env.getString("client_assertion", "value");
		String clientSecret = env.getString("client", "client_secret");

		try {
			SignedJWT jwt = SignedJWT.parse(clientAssertionString);
			boolean isValid = verifyHMACSignature(jwt, clientSecret);
			if(isValid) {
				logSuccess("Client assertion signature is valid");
				return env;
			} else {
				throw error("Client assertion signature is invalid",
							args("client_assertion", clientAssertionString, "client_secret", clientSecret));
			}
		} catch (JOSEException ex) {
			throw error("Failed to validate client assertion", ex,
						args("client_assertion", clientAssertionString, "client_secret", clientSecret));
		} catch (ParseException ex) {
			throw error("Invalid client assertion", ex, args("client_assertion", clientAssertionString));
		}
	}
}
