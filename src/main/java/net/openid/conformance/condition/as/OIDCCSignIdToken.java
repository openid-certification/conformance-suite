package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractSignJWT;
import net.openid.conformance.testmodule.Environment;

public class OIDCCSignIdToken extends AbstractSignJWT {

	@Override
	@PreEnvironment(required = { "id_token_claims", "server_jwks", "client"}, strings = {"signing_algorithm" })
	@PostEnvironment(strings = "id_token", required = {"all_issued_id_tokens"})
	public Environment evaluate(Environment env) {
		JsonObject claims = env.getObject("id_token_claims");
		JsonObject jwks = env.getObject("server_jwks");
		String signingAlg = env.getString("client", "id_token_signed_response_alg");
		if(signingAlg==null || signingAlg.isEmpty()) {
			//use the default
			signingAlg = env.getString("signing_algorithm");
		}
		JsonObject client = env.getObject("client");
		if("none".equals(signingAlg)) {
			String signed = signWithAlgNone(claims.toString());
			logSuccessByJWTType(env, null, null, null, signed, null);
		} else{
			JWK selectedKey = selectOrCreateKey(jwks, signingAlg, client);
			signJWTUsingKey(env, claims, selectedKey, signingAlg);
		}
		//keep track of all issued id_tokens to be used for logout
		String idToken = env.getString("id_token");
		if(!env.containsObject("all_issued_id_tokens")) {
			JsonObject allIdTokens = new JsonObject();
			env.putObject("all_issued_id_tokens", allIdTokens);
		}
		JsonObject allIdTokens = env.getObject("all_issued_id_tokens");
		//because you can't add JsonArrays to env
		allIdTokens.addProperty(idToken, "1");

		return env;
	}

	@Override
	protected void logSuccessByJWTType(Environment env, JWTClaimsSet claimSet, JWK jwk, JWSHeader header, String jws, JsonObject verifiableObj) {
		env.putString("id_token", jws);
		logSuccess("Signed the ID token",
			args("id_token", (verifiableObj!=null?verifiableObj:jws),
					"algorithm", (header!=null?header.getAlgorithm().getName():"none"),
					"key", (jwk!=null?jwk.toJSONString():"none")));
	}

}
