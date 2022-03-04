package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractAddClaimToAuthorizationEndpointRequest;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AddUnverifiedClaimsToAuthorizationEndpointRequest extends AbstractAddClaimToAuthorizationEndpointRequest {

	/**
	 * unverified_claims_to_request must be like
	 * {
	 *     "a claim name": {
	 *         "location":"ID_TOKEN",
	 *         "value":"some value",
	 *         "essential:true
	 *     }
	 * }
	 * essential is optional
	 * @param env
	 * @return
	 */
	@Override
	@PreEnvironment(required = {"authorization_endpoint_request", "server", "unverified_claims_to_request"})
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonElement claimsSupportedElement = env.getElementFromObject("server", "claims_supported");
		if(claimsSupportedElement==null) {
			throw error("claims_supported element in server configuration is required for this test");
		}
		JsonObject claimsToRequest = env.getObject("unverified_claims_to_request");
		for(String claimName : claimsToRequest.keySet()) {
			JsonObject claimInfo = claimsToRequest.get(claimName).getAsJsonObject();
			String value = null;
			if(claimInfo.has("value")) {
				value = OIDFJSON.getString(claimInfo.get("value"));
			}
			boolean essential = claimInfo.has("essential")? OIDFJSON.getBoolean(claimInfo.get("essential")):false;
			addClaim(env, LocationToRequestClaim.ID_TOKEN, claimName, value, essential);
			addClaim(env, LocationToRequestClaim.USERINFO, claimName, value, essential);
		}

		return env;
	}

}
