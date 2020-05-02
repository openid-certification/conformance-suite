package net.openid.conformance.condition.as.logout;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractSignJWT;
import net.openid.conformance.testmodule.Environment;

public class OIDCCSignLogoutToken extends AbstractSignJWT {

	@Override
	@PreEnvironment(required = { "logout_token_claims", "server_jwks", "client"}, strings = {"signing_algorithm" })
	@PostEnvironment(strings = "logout_token")
	public Environment evaluate(Environment env) {
		JsonObject claims = env.getObject("logout_token_claims");
		JsonObject jwks = env.getObject("server_jwks");
		String signingAlg = getAlg(env);
		JsonObject client = env.getObject("client");

		JWK selectedKey = selectOrCreateKey(jwks, signingAlg, client);
		signJWTUsingKey(env, claims, selectedKey, signingAlg);

		return env;
	}

	protected String getAlg(Environment env) {
		String signingAlg = env.getString("client", "id_token_signed_response_alg");
		if(signingAlg==null || signingAlg.isEmpty()) {
			//use the default
			signingAlg = env.getString("signing_algorithm");
		}

		if("none".equals(signingAlg)) {
			throw error("Algorithm 'none' cannot be used for logout tokens");
		}
		return signingAlg;
	}

	@Override
	protected void logSuccessByJWTType(Environment env, JWTClaimsSet claimSet, JWK jwk, JWSHeader header, String jws, JsonObject verifiableObj) {
		env.putString("logout_token", jws);
		logSuccess("Signed the logout token",
			args("logout_token", (verifiableObj!=null?verifiableObj:jws),
					"algorithm", (header!=null?header.getAlgorithm().getName():"none"),
					"key", (jwk!=null?jwk.toJSONString():"none")));
	}

}
