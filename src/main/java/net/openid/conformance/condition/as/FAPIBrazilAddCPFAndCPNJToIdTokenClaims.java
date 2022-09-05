package net.openid.conformance.condition.as;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilAddCPFAndCPNJToIdTokenClaims extends AbstractFAPIBrazilAddCPFAndCPNJToGeneratedClaims {

	@Override
	@PreEnvironment(required = { "id_token_claims", "authorization_request_object" })
	public Environment evaluate(Environment env) {
		if(addClaims(env, "id_token_claims", "id_token")) {
			logSuccess("Added claims to id_token claims", args("id_token_claims", env.getObject("id_token_claims")));
		}
		return env;
	}

}
