package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractSignJWT;
import net.openid.conformance.testmodule.Environment;

public class OIDCCSignClaimsEndpointResponse extends AbstractSignJWT {

	@Override
	@PreEnvironment(required = { "distributed_claims", "server_jwks", "client"}, strings = {"signing_algorithm" })
	@PostEnvironment(strings = "distributed_claims_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonObject claims = env.getObject("distributed_claims");
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

		return env;
	}

	@Override
	protected void logSuccessByJWTType(Environment env, JWTClaimsSet claimSet, JWK jwk, JWSHeader header, String jws, JsonObject verifiableObj) {
		env.putString("distributed_claims_endpoint_response", jws);
		logSuccess("Signed the claims endpoint response",
			args("claims_response", (verifiableObj!=null?verifiableObj:jws),
					"algorithm", (header!=null?header.getAlgorithm().getName():"none"),
					"key", (jwk!=null?jwk.toJSONString():"none")));
	}

}
