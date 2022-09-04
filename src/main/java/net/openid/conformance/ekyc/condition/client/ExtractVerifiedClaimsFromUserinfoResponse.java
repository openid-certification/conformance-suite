package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractVerifiedClaimsFromUserinfoResponse extends AbstractCondition {

	/**
	 * Optionally adds verified_claims_response to env
	 * verified_claims_response is like
	 *   {
	 *    "id_token": jsonelement,
	 *    "userinfo": jsonelement
	 *   }
	 * @param env
	 * @return
	 */

	@Override
	@PreEnvironment(required = {"server", "authorization_endpoint_request"}, strings = "resource_endpoint_response")
	public Environment evaluate(Environment env) {
		String userinfoResponse = env.getString("resource_endpoint_response");
		JsonObject parsedUserinfo = JsonParser.parseString(userinfoResponse).getAsJsonObject();
		JsonElement verifiedClaimsElement = parsedUserinfo.get("verified_claims");
		if(verifiedClaimsElement!=null) {
			JsonObject envHolder = env.getObject("verified_claims_response");
			if(envHolder == null){
				envHolder = new JsonObject();
				env.putObject("verified_claims_response", envHolder);
			}
			envHolder.add("userinfo", verifiedClaimsElement);
			return env;
		}
		log("userinfo response does not contain verified_claims");
		return env;
	}
}
