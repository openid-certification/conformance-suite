package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddIssAndAudToUserInfoResponse extends AbstractCondition {

	/**
	 * Should be used when signing the userinfo response
	 * @param env
	 * @return
	 */
	@Override
	@PreEnvironment(required = { "user_info_endpoint_response", "client"})
	@PostEnvironment(required = "user_info_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonObject userinfoResponse = env.getObject("user_info_endpoint_response");
		String clientId = env.getString("client", "client_id");
		String issuer = env.getString("issuer");
		userinfoResponse.addProperty("iss", issuer);
		userinfoResponse.addProperty("aud", clientId);
		env.putObject("user_info_endpoint_response", userinfoResponse);
		log("Added iss and aud claims to userinfo response", args("iss", issuer, "aud", clientId));
		return env;
	}
}
