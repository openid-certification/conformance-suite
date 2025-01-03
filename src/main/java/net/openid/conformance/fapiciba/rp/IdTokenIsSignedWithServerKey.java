package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonElement;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractVerifyJwsSignature;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.text.ParseException;

public class IdTokenIsSignedWithServerKey extends AbstractVerifyJwsSignature {

	@Override
	@PreEnvironment(required = "server_jwks")
	public Environment evaluate(Environment env) {

		try {
			JsonElement idTokenHintElement = env.getElementFromObject("backchannel_request_object", "claims.id_token_hint");
			if(idTokenHintElement == null){
				throw error("id_token_hint is not present in the backchannel request object.");
			}
			SignedJWT idTokenHint = SignedJWT.parse(OIDFJSON.getString(idTokenHintElement));
			JWKSet serverJwks = JWKSet.parse(env.getObject("server_jwks").toString());
			if(!verifySignature(idTokenHint, serverJwks)){
				throw error("The provided id_token_hint is not signed with the currently configured server sig key.");
			}
		} catch (ParseException e) {
			throw error("Unable to parse id_token_hint. Either it's not a well-formed, signed JWT, or it might be encrypted", e);
		} catch (JOSEException e) {
			throw error("An error occurred while verifying the signature", e);
		}

		logSuccess("The id_token is signed with the currently configured server key");

		return env;
	}

}
