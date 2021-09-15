package net.openid.conformance.condition.client.ekyc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractAddClaimToAuthorizationEndpointRequest;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import springfox.documentation.spring.web.json.Json;

public class AddOnlyOneSimpleVerifiedClaimToAuthorizationEndpointRequest extends AbstractAddClaimToAuthorizationEndpointRequest {


	@Override
	@PreEnvironment(required = {"server", "authorization_endpoint_request"})
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonElement verifiedClaimsSupportedElement = env.getElementFromObject("server", "claims_in_verified_claims_supported");
		if(verifiedClaimsSupportedElement==null) {
			throw error("claims_in_verified_claims_supported element in server configuration is required for this test");
		}
		JsonObject verifiedClaims = new JsonObject();
		JsonObject verification = new JsonObject();
		verification.add("trust_framework", JsonNull.INSTANCE);
		verifiedClaims.add("verification", verification);
		JsonObject claims = new JsonObject();
		JsonElement claimName = verifiedClaimsSupportedElement.getAsJsonArray().get(0);
		claims.add(OIDFJSON.getString(claimName), JsonNull.INSTANCE);

		verifiedClaims.add("claims", claims);
		addVerifiedClaims(env, verifiedClaims);
		logSuccess("Added verified claims to authorization request",
			args("authorization_endpoint_request", env.getObject("authorization_endpoint_request").get("claims"),
			"verified_claims", verifiedClaims));
		return env;
	}

	protected void addVerifiedClaims(Environment env, JsonObject verifiedClaims) {
		JsonElement topLevelClaimsElement = env.getElementFromObject("authorization_endpoint_request", "claims");
		JsonObject topLevelClaimsObject = null;
		if(topLevelClaimsElement==null) {
			topLevelClaimsElement = new JsonObject();
			env.getObject("authorization_endpoint_request").add("claims", topLevelClaimsElement);
		} else {
			topLevelClaimsObject = topLevelClaimsElement.getAsJsonObject();
		}
		if(topLevelClaimsObject.has("id_token")) {
			topLevelClaimsObject.get("id_token").getAsJsonObject().add("verified_claims", verifiedClaims);
		}
		if(topLevelClaimsObject.has("userinfo")) {
			topLevelClaimsObject.get("userinfo").getAsJsonObject().add("verified_claims", verifiedClaims);
		}
	}

}
