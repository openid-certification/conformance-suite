package net.openid.conformance.openid.federation.client;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractSignJWT;
import net.openid.conformance.openid.federation.EntityUtils;
import net.openid.conformance.testmodule.Environment;

public class SignEntityStatementWithClientKeys extends AbstractSignJWT {

	@Override
	protected JOSEObjectType getMediaType() {
		return EntityUtils.ENTITY_STATEMENT_TYPE;
	}

	@Override
	@PreEnvironment(required = { "entity_configuration_claims", "client_jwks" })
	@PostEnvironment(strings = "signed_entity_statement")
	public Environment evaluate(Environment env) {
		JsonObject claims = env.getObject("entity_configuration_claims");
		JsonObject jwks = env.getObject("client_jwks");
		return signJWT(env, claims, jwks, true);
	}

	@Override
	protected void logSuccessByJWTType(Environment env, JWTClaimsSet claimSet, JWK jwk, JWSHeader header, String jws, JsonObject verifiableObj) {
		env.putString("signed_entity_statement", jws);
		logSuccess("Signed the entity statement", args("signed_entity_statement", verifiableObj));
	}

}
