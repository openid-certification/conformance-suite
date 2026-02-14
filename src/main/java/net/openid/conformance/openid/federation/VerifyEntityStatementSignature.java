package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractVerifyJwsSignature;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class VerifyEntityStatementSignature extends AbstractVerifyJwsSignature {

	@Override
	@PreEnvironment(required = { "ec_jwks", "federation_response_jwt"} )
	public Environment evaluate(Environment env) {

		JsonObject entityStatementJwks = env.getObject("ec_jwks");
		String entityStatementJwt = OIDFJSON.getString(env.getElementFromObject("federation_response_jwt", "value"));
		verifyJwsSignature(entityStatementJwt, entityStatementJwks, "entity_statement", true, "entity statement");
		return env;
	}

	// Widens access from protected to public for TrustChainVerifier
	@Override
	public boolean verifySignature(SignedJWT jwt, JWKSet jwkSet) throws JOSEException {
		return super.verifySignature(jwt, jwkSet);
	}
}
