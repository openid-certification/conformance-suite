package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Must be called after a FilterUserInfoForScopes call
 * Use to add userinfo claims filtered by scopes to id_token
 */
public class AddUserinfoClaimsToIdTokenClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "id_token_claims", "user_info_endpoint_response" })
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {

		JsonObject userInfoEndpointResponse = env.getObject("user_info_endpoint_response");
		JsonObject idTokenClaims = env.getObject("id_token_claims");
		for(String claimName : userInfoEndpointResponse.keySet()) {
			idTokenClaims.add(claimName, userInfoEndpointResponse.get(claimName));
		}

		env.putObject("id_token_claims", idTokenClaims);

		log("Added userinfo claims to ID Token Claims", idTokenClaims);

		return env;

	}

}
