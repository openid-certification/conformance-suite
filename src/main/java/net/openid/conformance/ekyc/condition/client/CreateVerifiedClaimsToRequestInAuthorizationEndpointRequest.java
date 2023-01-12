package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractAddClaimToAuthorizationEndpointRequest;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class CreateVerifiedClaimsToRequestInAuthorizationEndpointRequest extends AbstractAddClaimToAuthorizationEndpointRequest {

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
	@PreEnvironment(required = {"server"})
	@PostEnvironment(required = "verified_claims_to_request")
	public Environment evaluate(Environment env) {
		JsonElement verifiedClaimsSupportedElement = env.getElementFromObject("server", "claims_in_verified_claims_supported");
		if(verifiedClaimsSupportedElement==null) {
			throw error("claims_in_verified_claims_supported element in server configuration is required for this test");
		}
		JsonElement trustFrameworksSupportedElement = env.getElementFromObject("server", "trust_frameworks_supported");
		JsonArray trustFrameworks = null;
		if(trustFrameworksSupportedElement!=null) {
			trustFrameworks = trustFrameworksSupportedElement.getAsJsonArray();

		}
		//TODO if multiple claims and trust frameworks and document types are supported, we cannot tell what combination
		// of those will work by just looking at the server discovery document.
		// Users should be able to provide these in config
		JsonArray verifiedClaimsSupportedArray = verifiedClaimsSupportedElement.getAsJsonArray();
		int matchedClaimCount = 0;
		JsonObject verifiedClaimsToRequest = new JsonObject();
		JsonObject verification = new JsonObject();
		if(trustFrameworks!=null && trustFrameworks.size()>0) {
			verification.addProperty("trust_framework", OIDFJSON.getString(trustFrameworks.get(0)));
		} else {
			verification.add("trust_framework", JsonNull.INSTANCE);
		}
		verifiedClaimsToRequest.add("verification", verification);
		JsonObject claims = new JsonObject();
		for(JsonElement claimName : verifiedClaimsSupportedArray) {
			claims.add(OIDFJSON.getString(claimName), JsonNull.INSTANCE);
			matchedClaimCount++;
			if(matchedClaimCount>1) {
				break;
			}
		}
		verifiedClaimsToRequest.add("claims", claims);
		env.putObject("verified_claims_to_request", verifiedClaimsToRequest);
		logSuccess("Added verified claims to authorization request", args("verified_claims_to_request", verifiedClaimsToRequest));
		return env;
	}

}
