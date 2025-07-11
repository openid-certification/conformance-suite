package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddVerifiedClaimsFromUserinfoToAuthorizationEndpointRequest extends AbstractCreateVerifiedClaimsRequestFromResponseObject {


	@Override
	@PreEnvironment(required = {"config", "authorization_endpoint_request"})
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonElement userinfo = env.getElementFromObject("config", "ekyc.userinfo");
		if(userinfo==null){
			throw error("User provided userinfo data is not set in configuration");
		}
		JsonElement verifiedClaims = createVerifiedClaimsRequestFromUserinfo(userinfo.getAsJsonObject());
		JsonObject authzEndpointRequest = env.getObject("authorization_endpoint_request");
		JsonObject claims = null;
		if(authzEndpointRequest.has("claims")) {
			claims = authzEndpointRequest.get("claims").getAsJsonObject();
		} else {
			claims = new JsonObject();
		}
		if(!claims.has("userinfo")) {
			claims.add("userinfo", new JsonObject());
		}
		claims.get("userinfo").getAsJsonObject().add("verified_claims", verifiedClaims);
		authzEndpointRequest.add("claims", claims);
		logSuccess("Added verified_claims based on provided userinfo to authorization request",
			args("authorization_endpoint_request",
				env.getObject("authorization_endpoint_request")));
		return env;
	}

	protected JsonElement createVerifiedClaimsRequestFromUserinfo(JsonObject userinfo) {
		JsonElement verifiedClaimsElementInUserinfo = userinfo.get("verified_claims");
		if(verifiedClaimsElementInUserinfo==null){
			throw error("userinfo must contain a verified_claims element");
		}
		if(verifiedClaimsElementInUserinfo.isJsonObject()) {
			JsonObject newRequestElement = createVerifiedClaimsFromVerifiedClaimsObject(verifiedClaimsElementInUserinfo.getAsJsonObject());
			return newRequestElement;
		} else if(verifiedClaimsElementInUserinfo.isJsonArray()) {
			JsonArray array = verifiedClaimsElementInUserinfo.getAsJsonArray();
			JsonArray requestClaims = new JsonArray();
			for(JsonElement element : array) {
				JsonObject newRequestElement = createVerifiedClaimsFromVerifiedClaimsObject(element.getAsJsonObject());
				requestClaims.add(newRequestElement);
			}
			return requestClaims;
		} else {
			throw error("Unexpected verified_claims in userinfo, must be either an array or object",
						args("userinfo", userinfo));
		}
	}

	protected JsonObject createVerifiedClaimsFromVerifiedClaimsObject(JsonObject verifiedClaimsObjectFromUserinfo) {
		JsonObject rv = new JsonObject();
		JsonObject claims = new JsonObject();
		JsonObject claimsInUserinfo = verifiedClaimsObjectFromUserinfo.get("claims").getAsJsonObject();
		for(String claimName : claimsInUserinfo.keySet()) {
			claims.add(claimName, JsonNull.INSTANCE);
		}
		rv.add("claims", claims);
		JsonObject verification = createVerificationClaims(verifiedClaimsObjectFromUserinfo.getAsJsonObject("verification"));
		rv.add("verification", verification);
		return rv;
	}


}
