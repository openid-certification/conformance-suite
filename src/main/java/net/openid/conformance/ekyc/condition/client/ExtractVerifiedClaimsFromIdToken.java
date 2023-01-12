package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractVerifiedClaimsFromIdToken extends AbstractCondition {

	/**
	 * Optionally adds verified_claims_response to env
	 * verified_claims_response is like
	 *   {
	 *    "id_token": jsonelement,
	 *    "userinfo": jsonelement
	 *   }
	 * @param env
	 * @return
	 */

	@Override
	@PreEnvironment(required = {"server", "authorization_endpoint_request", "id_token"})
	public Environment evaluate(Environment env) {
		JsonElement verifiedClaimsElement = env.getElementFromObject("id_token", "claims.verified_claims");
		if(verifiedClaimsElement!=null) {
			JsonObject envHolder = env.getObject("verified_claims_response");
			if(envHolder == null){
				envHolder = new JsonObject();
				env.putObject("verified_claims_response", envHolder);
			}
			envHolder.add("id_token", verifiedClaimsElement);
			return env;
		}
		throw error("id_token does not contain verified_claims");
	}

}
