package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public abstract class AbstractAddVerifiedClaimToAuthorizationEndpointRequest extends AbstractCondition {

	/**
	 * Retrieve configured list of server supported verified claims request list or first one in the server supported
	 * list if not configured
	 * @param env Environment
	 * @return JsonArray of list
	 */
	protected JsonArray getVerifiedClaimsRequestList(Environment env) {
		JsonElement verifiedClaimsSupportedElement = env.getElementFromObject("server", "claims_in_verified_claims_supported");
		if(verifiedClaimsSupportedElement==null) {
			throw error("claims_in_verified_claims_supported element in server configuration is required for this test");
		}
		JsonArray verifiedClaimsSupportedList = verifiedClaimsSupportedElement.getAsJsonArray();

		JsonElement verifiedClaimsNamesElement = env.getElementFromObject("config","ekyc.verified_claims_names");
		JsonArray verifiedClaimsNamesArray;
		JsonArray requestedVerifiedClaimsList = new JsonArray();
		if(null != verifiedClaimsNamesElement) {
			if(verifiedClaimsNamesElement.isJsonArray()) {
				verifiedClaimsNamesArray = verifiedClaimsNamesElement.getAsJsonArray();
			} else if(verifiedClaimsNamesElement.isJsonPrimitive()) {
				verifiedClaimsNamesArray = OIDFJSON.packJsonElementIntoJsonArray(verifiedClaimsNamesElement);
			} else {
				throw error("ekyc.verified_claims_names is not JSON array or primitive", args("ekyc.verified_claims_names", verifiedClaimsNamesElement));
			}
			for(JsonElement claimName : verifiedClaimsNamesArray) {
				if(!verifiedClaimsSupportedList.contains(claimName)) {
					continue;
				}
				requestedVerifiedClaimsList.add(claimName);
			}
		}

		if(requestedVerifiedClaimsList.isEmpty()) {
			requestedVerifiedClaimsList.add(verifiedClaimsSupportedList.get(0));
		}
		return requestedVerifiedClaimsList;
	}

	protected void addVerifiedClaims(Environment env, JsonObject verifiedClaims, boolean toIdToken, boolean toUserinfo) {
		JsonElement topLevelClaimsElement = env.getElementFromObject("authorization_endpoint_request", "claims");
		if (topLevelClaimsElement == null) {
			topLevelClaimsElement = new JsonObject();
			env.getObject("authorization_endpoint_request").add("claims", topLevelClaimsElement);
		}
		JsonObject topLevelClaimsObject = topLevelClaimsElement.getAsJsonObject();

		if (toIdToken) {
			if (!topLevelClaimsObject.has("id_token")) {
				topLevelClaimsObject.add("id_token", new JsonObject());
			}
			topLevelClaimsObject.get("id_token").getAsJsonObject().add("verified_claims", verifiedClaims);
		}

		if (toUserinfo) {
			if (!topLevelClaimsObject.has("userinfo")) {
				topLevelClaimsObject.add("userinfo", new JsonObject());
			}
			topLevelClaimsObject.get("userinfo").getAsJsonObject().add("verified_claims", verifiedClaims);
		}
	}

}
