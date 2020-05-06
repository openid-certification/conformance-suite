package net.openid.conformance.condition.as.logout;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractSignJWT;
import net.openid.conformance.testmodule.Environment;

public class OIDCCSignLogoutTokenWithAlgNone extends AbstractSignJWT {

	@Override
	@PreEnvironment(required = { "logout_token_claims"})
	@PostEnvironment(strings = "logout_token")
	public Environment evaluate(Environment env) {
		JsonObject claims = env.getObject("logout_token_claims");
		String signed = signWithAlgNone(claims.toString());
		logSuccessByJWTType(env, null, null, null, signed, null);
		env.putString("logout_token", signed);
		return env;
	}

	@Override
	protected void logSuccessByJWTType(Environment env, JWTClaimsSet claimSet, JWK jwk, JWSHeader header, String jws, JsonObject verifiableObj) {
		env.putString("logout_token", jws);
		logSuccess("Signed the logout token using algorithm 'none'",
			args("logout_token", (verifiableObj!=null?verifiableObj:jws), "algorithm", "none"));
	}

}
