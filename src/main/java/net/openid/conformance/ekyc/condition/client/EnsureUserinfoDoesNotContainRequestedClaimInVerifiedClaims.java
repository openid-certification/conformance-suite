package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureUserinfoDoesNotContainRequestedClaimInVerifiedClaims extends AbstractCondition {

	//TODO parsing userinfo response from resource_endpoint_response is not ideal.
	// userinfo responses must be processed and extracted properly, e.g userinfo response might be signed or encrypted
	@Override
	@PreEnvironment(required = {"authorization_endpoint_request", "verified_claims_response"})
	public Environment evaluate(Environment env) {
		JsonElement verifiedClaimsElementFromResponse = env.getElementFromObject("verified_claims_response", "userinfo");
		JsonElement requestedVerifiedClaimsElement = env.getElementFromObject("authorization_endpoint_request", "claims.userinfo.verified_claims");

		if(verifiedClaimsElementFromResponse==null) {
			throw error("verified_claims not found");
		}
		validateResponseAgainstRequestedVerifiedClaims(requestedVerifiedClaimsElement, verifiedClaimsElementFromResponse);

		logSuccess("Verified claims do not contain requested claims", args("response", verifiedClaimsElementFromResponse,
			"requested", requestedVerifiedClaimsElement));
		return env;
	}

	/**
	 *
	 * @param requestedVerifiedClaimsElement json object or array
	 * @param returnedVerifiedClaimsElement json object or array
	 */
	protected void validateResponseAgainstRequestedVerifiedClaims(JsonElement requestedVerifiedClaimsElement, JsonElement returnedVerifiedClaimsElement) {
		if(requestedVerifiedClaimsElement.isJsonObject()) {
			validateResponseAgainstSingleRequestedVerifiedClaims(requestedVerifiedClaimsElement.getAsJsonObject(), returnedVerifiedClaimsElement);
		} else if(requestedVerifiedClaimsElement.isJsonArray()) {
			for(JsonElement element : requestedVerifiedClaimsElement.getAsJsonArray()) {
				if(element.isJsonObject()) {
					JsonObject jsonObject = element.getAsJsonObject();
					validateResponseAgainstSingleRequestedVerifiedClaims(jsonObject, returnedVerifiedClaimsElement);
				} else {
					throw error("Unexpected element in verified_claims array in request", args("element", element));
				}
			}
		} else {
			throw error("Unexpected verified_claims element in request. Must be either an array or object",
				args("element", requestedVerifiedClaimsElement));
		}
	}


	protected void validateResponseAgainstSingleRequestedVerifiedClaims(JsonObject requestedVerifiedClaims, JsonElement returnedVerifiedClaimsElement) {
		if(returnedVerifiedClaimsElement.isJsonArray()) {
			for(JsonElement element : returnedVerifiedClaimsElement.getAsJsonArray()) {
				if(element.isJsonObject()) {
					JsonObject jsonObject = element.getAsJsonObject();
					validateOneOnOne(requestedVerifiedClaims, jsonObject);
				} else {
					throw error("Unexpected element in verified_claims array in request", args("element", element));
				}
			}
		} else if(returnedVerifiedClaimsElement.isJsonObject()) {
			validateOneOnOne(requestedVerifiedClaims, returnedVerifiedClaimsElement.getAsJsonObject());
		} else {
			throw error("Returned verified_claims element is neither an array or object",
				args("element", returnedVerifiedClaimsElement));
		}
	}

	protected void validateOneOnOne(JsonObject requestedVerifiedClaims, JsonObject returnedVerifiedClaims){
		JsonObject requestedClaims = requestedVerifiedClaims.get("claims").getAsJsonObject();
		if(returnedVerifiedClaims.has("claims")) {
			JsonObject returnedClaims = returnedVerifiedClaims.get("claims").getAsJsonObject();
			boolean foundAtLeastOneMatch = false;
			JsonArray matchList = new JsonArray();
			for(String key : requestedClaims.keySet()) {
				if(returnedClaims.has(key)) {
					foundAtLeastOneMatch = true;
					matchList.add(key);
				}
			}
			if(foundAtLeastOneMatch) {
				//should not return any requested claims since values are made up
				throw error("Found requested claims when none are expected", args("claims", matchList));
			}
		}
	}
}
