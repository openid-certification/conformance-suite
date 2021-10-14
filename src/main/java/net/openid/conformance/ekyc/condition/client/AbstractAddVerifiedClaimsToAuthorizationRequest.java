package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractAddVerifiedClaimsToAuthorizationRequest extends AbstractCondition {

	protected void addClaims(Environment env, JsonObject verifiedClaim) {
		JsonElement topLevelClaimsElement = env.getElementFromObject("authorization_endpoint_request", "claims");
		JsonObject topLevelClaimsObject = null;
		if(topLevelClaimsElement==null) {
			topLevelClaimsElement = new JsonObject();
			env.getObject("authorization_endpoint_request").add("claims", topLevelClaimsElement);
		} else {
			topLevelClaimsObject = topLevelClaimsElement.getAsJsonObject();
		}

		if(topLevelClaimsObject.has("verified_claims")) {
			JsonElement verifiedClaimsElement = topLevelClaimsObject.get("verified_claims");
			if(verifiedClaimsElement.isJsonArray()) {
				verifiedClaimsElement.getAsJsonArray().add(verifiedClaim);
			} else if(verifiedClaimsElement.isJsonObject()) {
				//convert to array
				JsonArray verifiedClaimsArray = new JsonArray();
				verifiedClaimsArray.add(verifiedClaimsElement);
				verifiedClaimsArray.add(verifiedClaim);
				topLevelClaimsObject.add("verified_claims", verifiedClaimsArray);
			} else {
				throw error("verified_claims is neither object nor array");
			}
		} else {
			//add as json object
			topLevelClaimsObject.add("verified_claims", verifiedClaim);
		}
	}

}
