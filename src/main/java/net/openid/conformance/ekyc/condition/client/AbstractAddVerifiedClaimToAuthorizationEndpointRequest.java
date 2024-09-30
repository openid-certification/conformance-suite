package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractAddVerifiedClaimToAuthorizationEndpointRequest extends AbstractCondition {

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
