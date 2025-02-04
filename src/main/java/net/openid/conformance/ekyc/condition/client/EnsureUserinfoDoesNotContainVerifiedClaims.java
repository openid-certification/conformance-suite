package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureUserinfoDoesNotContainVerifiedClaims extends AbstractCondition {

	//TODO parsing userinfo response from resource_endpoint_response is not ideal.
	// userinfo responses must be processed and extracted properly, e.g userinfo response might be signed or encrypted
	@Override
	@PreEnvironment(strings = {"resource_endpoint_response"})
	public Environment evaluate(Environment env) {
		String userinfoResponse = env.getString("resource_endpoint_response");
		JsonObject parsedUserinfo = JsonParser.parseString(userinfoResponse).getAsJsonObject();
		if(parsedUserinfo.has("verified_claims")) {
			JsonObject verifiedClaims = parsedUserinfo.getAsJsonObject("verified_claims");
			if(verifiedClaims.has("claims")) {
				JsonObject claims = verifiedClaims.getAsJsonObject("claims");
				if(!claims.isEmpty()) { // claims may be empty per https://openid.net/specs/openid-ida-verified-claims-1_0.htm - 5.3
					throw error("userinfo response unexpectedly contains verified_claims",
						args("userinfo", parsedUserinfo));
				}
			}
		}
		logSuccess("userinfo does not contain verified_claims as expected");
		return env;
	}
}
