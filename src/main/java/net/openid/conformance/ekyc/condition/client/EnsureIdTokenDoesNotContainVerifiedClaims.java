package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureIdTokenDoesNotContainVerifiedClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"id_token"})
	public Environment evaluate(Environment env) {
		JsonElement verifiedClaimsElement = env.getElementFromObject("id_token", "claims.verified_claims");
		if(verifiedClaimsElement!=null) {
			JsonObject verifiedClaims = verifiedClaimsElement.getAsJsonObject();
			if(verifiedClaims.has("claims")) { // look in the claims object
				JsonObject claims = verifiedClaims.getAsJsonObject("claims");
				if(!claims.isEmpty()) { // claims can be empty per https://openid.net/specs/openid-ida-verified-claims-1_0.htm - 5.3
					throw error("id_token unexpectedly contains verified_claims",
						args("verified_claims", verifiedClaimsElement));
				}
			}
		}
		logSuccess("id_token does not contain verified_claims as expected");
		return env;
	}

}
