package net.openid.conformance.openid.federation.client;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractSignJWT;
import net.openid.conformance.testmodule.Environment;

public class SignEntityStatementWithTrustAnchorKeys extends AbstractSignJWT {

	@Override
	@PreEnvironment(required = { "entity_statement_claims", "trust_anchor_jwks" })
	@PostEnvironment(strings = "signed_entity_statement")
	public Environment evaluate(Environment env) {
		JsonObject claims = env.getObject("entity_statement_claims");
		JsonObject jwks = env.getObject("trust_anchor_jwks");
		return signJWT(env, claims, jwks);
	}

	@Override
	protected void logSuccessByJWTType(Environment env, JWTClaimsSet claimSet, JWK jwk, JWSHeader header, String jws, JsonObject verifiableObj) {
		env.putString("signed_entity_statement", jws);
		logSuccess("Signed the entity statement", args("signed_entity_statement", verifiableObj));
	}

}
