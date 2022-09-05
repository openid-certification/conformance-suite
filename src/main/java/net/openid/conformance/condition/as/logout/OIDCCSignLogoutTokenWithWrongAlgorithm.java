package net.openid.conformance.condition.as.logout;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class OIDCCSignLogoutTokenWithWrongAlgorithm extends OIDCCSignLogoutToken {

	@Override
	@PreEnvironment(required = { "logout_token_claims", "server_jwks", "client"}, strings = {"signing_algorithm" })
	@PostEnvironment(strings = "logout_token")
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}

	@Override
	protected String getAlg(Environment env) {
		String signingAlg = env.getString("client", "id_token_signed_response_alg");
		if(signingAlg==null || signingAlg.isEmpty()) {
			//use the default
			signingAlg = env.getString("signing_algorithm");
		}
		if("RS256".equals(signingAlg)) {
			return "ES256";
		} else {
			return "RS256";
		}
	}

	@Override
	protected void logSuccessByJWTType(Environment env, JWTClaimsSet claimSet, JWK jwk, JWSHeader header, String jws, JsonObject verifiableObj) {
		env.putString("logout_token", jws);
		logSuccess("Signed the logout token with a wrong algorithm",
			args("logout_token", (verifiableObj!=null?verifiableObj:jws),
					"algorithm", (header!=null?header.getAlgorithm().getName():"none"),
					"configured_algorithm", super.getAlg(env),
					"key", (jwk!=null?jwk.toJSONString():"none")));
	}

}
