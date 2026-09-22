package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

/**
 * Serializes the client assertion claims as an unsecured JWT ("alg": "none", empty signature).
 */
public class CreateUnsecuredClientAuthenticationAssertion extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client_assertion_claims")
	@PostEnvironment(strings = "client_assertion")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("client_assertion_claims");

		try {
			String jwt = new PlainJWT(JWTClaimsSet.parse(claims.toString())).serialize();

			env.putString("client_assertion", jwt);

			log("Created an unsecured client assertion using the 'none' algorithm", args("client_assertion", jwt));

			return env;
		} catch (ParseException e) {
			throw error("Couldn't parse client assertion claims", e, args("client_assertion_claims", claims));
		}
	}

}
