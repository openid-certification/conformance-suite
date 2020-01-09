package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class VerifyScopesReturnedInAuthorizationEndpointIdToken extends AbstractVerifyScopesReturnedInClaims {

	@Override
	@PreEnvironment(required = { "authorization_endpoint_id_token", "authorization_endpoint_request" })
	public Environment evaluate(Environment env) {
		JsonElement claims = env.getElementFromObject("authorization_endpoint_id_token", "claims");
		return verifyScopesInClaims(env, claims, "authorization_endpoint_id_token");
	}
}
