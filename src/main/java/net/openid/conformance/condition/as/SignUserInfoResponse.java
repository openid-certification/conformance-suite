package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractSignJWT;
import net.openid.conformance.testmodule.Environment;

public class SignUserInfoResponse extends AbstractSignJWT {


	/**
	 * Requires userinfo_signed_response_alg. Use with skipIfElementMissing
	 *
	 * @param env
	 * @return
	 */
	@Override
	@PreEnvironment(required = { "user_info_endpoint_response", "server_jwks", "client" })
	@PostEnvironment(strings = "signed_user_info_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonObject claims = env.getObject("user_info_endpoint_response");
		JsonObject jwks = env.getObject("server_jwks");
		String signingAlg = env.getString("client", "userinfo_signed_response_alg");
		if("none".equals(signingAlg)) {
			String signed = signWithAlgNone(claims.toString());
			logSuccess("Signed the userinfo response with alg none", args("userinfo", signed));
			env.putString("signed_user_info_endpoint_response", signed);
			return env;
		} else {
			JsonObject client = env.getObject("client");

			JWK selectedKey = selectOrCreateKey(jwks, signingAlg, client);
			env = signJWTUsingKey(env, claims, selectedKey, signingAlg);
			return env;
		}
	}

	@Override
	protected void logSuccessByJWTType(Environment env, JWTClaimsSet claimSet, JWK jwk, JWSHeader header, String jws, JsonObject verifiableObj) {
		env.putString("signed_user_info_endpoint_response", jws);
		logSuccess("Signed the userinfo response", args("userinfo", verifiableObj));
	}

}
