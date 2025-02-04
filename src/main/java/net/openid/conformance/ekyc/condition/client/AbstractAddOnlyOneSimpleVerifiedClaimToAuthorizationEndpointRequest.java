package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public abstract class AbstractAddOnlyOneSimpleVerifiedClaimToAuthorizationEndpointRequest extends AbstractAddVerifiedClaimToAuthorizationEndpointRequest {
	@Override
	@PreEnvironment(required = {"server", "authorization_endpoint_request", "config"})
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonElement verifiedClaimsSupportedElement = env.getElementFromObject("server", "claims_in_verified_claims_supported");
		if(verifiedClaimsSupportedElement==null) {
			throw error("claims_in_verified_claims_supported element in server configuration is required for this test");
		}
		JsonArray verifiedClaimsSupportedList = verifiedClaimsSupportedElement.getAsJsonArray();
		JsonObject verifiedClaims = new JsonObject();
		JsonObject verification = new JsonObject();
		verification.add("trust_framework", JsonNull.INSTANCE);
		verifiedClaims.add("verification", verification);
		JsonObject claims = new JsonObject();

		JsonElement verifiedClaimsNamesElement = env.getElementFromObject("config","ekyc_verified_claims_names");
		JsonArray requestedVerifiedClaimsList;
		if(null != verifiedClaimsNamesElement) {
			if(verifiedClaimsNamesElement.isJsonArray()) {
				requestedVerifiedClaimsList = verifiedClaimsNamesElement.getAsJsonArray();
			} else if(verifiedClaimsNamesElement.isJsonPrimitive()) {
				requestedVerifiedClaimsList = new JsonArray();
				requestedVerifiedClaimsList.add(verifiedClaimsNamesElement);
			} else {
				throw error("ekyc_verified_claims_names is not JSON array or primitive", args("ekyc_verified_claims_names", verifiedClaimsNamesElement));
			}
			for(JsonElement claimName : requestedVerifiedClaimsList) {
				if(!verifiedClaimsSupportedList.contains(claimName)) {
					continue;
				}
				claims.add(OIDFJSON.getString(claimName), getClaimValue());
			}
		} else {
			claims.add(OIDFJSON.getString(verifiedClaimsSupportedList.get(0)), getClaimValue());
		}

		verifiedClaims.add("claims", claims);
		addVerifiedClaims(env, verifiedClaims, true, true);
		logSuccess("Added verified claims to authorization request",
			args("authorization_endpoint_request", env.getObject("authorization_endpoint_request"),
			"verified_claims", verifiedClaims));
		return env;
	}

	protected abstract JsonElement getClaimValue();
}
