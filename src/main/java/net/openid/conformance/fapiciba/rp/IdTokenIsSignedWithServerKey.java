package net.openid.conformance.fapiciba.rp;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractVerifyJwsSignature;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

public class IdTokenIsSignedWithServerKey extends AbstractVerifyJwsSignature {

	@Override
	@PreEnvironment(strings = "id_token_hint", required = "server_jwks")
	public Environment evaluate(Environment env) {

		try {
			SignedJWT idTokenHint = SignedJWT.parse(env.getString("id_token_hint"));
			JWKSet serverJwks = JWKSet.parse(env.getObject("server_jwks").toString());
			if(!verifySignature(idTokenHint, serverJwks)){
				throw error("The provided id_token_hint is not signed with the currently configured server sig key.");
			}
		} catch (ParseException e) {
			throw error("Unable to parse id_token_hint. Either it's not a well-formed id_token, or is it encrypted?", e);
		} catch (JOSEException e) {
			throw error("An error occurred while verifying the signature", e);
		}

		logSuccess("The id_token is signed with the currently configured server key");

		return env;
	}

}
