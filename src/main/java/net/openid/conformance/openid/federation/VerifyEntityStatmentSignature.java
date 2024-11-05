package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractVerifyJwsSignature;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.text.ParseException;

public class VerifyEntityStatmentSignature extends AbstractVerifyJwsSignature {

	@Override
	@PreEnvironment(required = { "server_jwks", "federation_response_jwt"} )
	public Environment evaluate(Environment env) {

		JsonObject entityStatementJwks = env.getObject("server_jwks");
		JsonObject entityStatementHeader = env.getElementFromObject("federation_response_jwt", "header").getAsJsonObject();
		try {
			String entityStatementB64 = OIDFJSON.getString(env.getElementFromObject("federation_response_jwt", "value"));
			SignedJWT entityStatement = SignedJWT.parse(entityStatementB64);
			JWKSet serverJwks = JWKSet.parse(entityStatementJwks.toString());
			if(!verifySignature(entityStatement, serverJwks)){
				throw error("The provided entity statement is not signed with the currently configured server sig key.",
					args("jwks", entityStatementJwks, "header", entityStatementHeader));
			}
		} catch (ParseException e) {
			throw error("Unable to parse entity statement. Either it's not a well-formed JWT, or is it encrypted?", e);
		} catch (JOSEException e) {
			throw error("An error occurred while verifying the signature", e);
		}

		logSuccess("The entity statement is signed with the currently configured server key",
			args("jwks", entityStatementJwks, "header", entityStatementHeader));

		return env;
	}

}
