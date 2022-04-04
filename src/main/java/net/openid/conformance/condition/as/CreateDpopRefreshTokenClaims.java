package net.openid.conformance.condition.as;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateDpopRefreshTokenClaims extends AbstractCreateJwtAccessTokenClaims {

	@Override
	@PreEnvironment(required = {"server"})
	@PostEnvironment(required = "dpop_refresh_token_claims")
	public Environment evaluate(Environment env) {
		return createJWTAccessTokenClaims(env, "dpop_refresh_token_claims");
	}
}
