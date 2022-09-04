package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractIdentityClaimsFromIdToken extends AbstractValidateOpenIdStandardClaims {

	@Override
	@PreEnvironment(required = "id_token")
	@PostEnvironment(required = "identity_claims")
	public Environment evaluate(Environment env) {

		JsonObject idTokenClaims = ValidateIdTokenStandardClaims.getIdTokenIdentityClaims(env);

		env.putObject("identity_claims", idTokenClaims);

		log("Extracted identity_claims from id_token", args("identity_claims", idTokenClaims));

		return env;
	}

}
